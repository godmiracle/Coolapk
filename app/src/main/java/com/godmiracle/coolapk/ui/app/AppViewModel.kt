package com.godmiracle.coolapk.ui.app

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.adapter.LoadingState
import com.godmiracle.coolapk.constant.Constants.LOADING_FAILED
import com.godmiracle.coolapk.logic.model.HomeFeedResponse
import com.godmiracle.coolapk.logic.repository.BlackListRepo
import com.godmiracle.coolapk.logic.repository.HistoryFavoriteRepo
import com.godmiracle.coolapk.logic.repository.NetworkRepo
import com.godmiracle.coolapk.ui.base.BaseAppViewModel
import com.godmiracle.coolapk.util.Event
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AppViewModel.Factory::class)
class AppViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    blackListRepo: BlackListRepo,
    historyRepo: HistoryFavoriteRepo,
    networkRepo: NetworkRepo
) : BaseAppViewModel(blackListRepo, historyRepo, networkRepo) {

    @AssistedFactory
    interface Factory {
        fun create(id: String): AppViewModel
    }

    var tabList: List<String>? = null
    var errMsg: String? = null
    var downloadUrl: String? = null

    // get download link params
    var appId: String? = null
    private var packageName: String? = null
    private var versionCode: String? = null
    val download = MutableLiveData<Event<Boolean>>()

    var appData: HomeFeedResponse.Data? = null
    val blockState = MutableLiveData<Event<Boolean>>()

    val searchState = MutableLiveData<Event<Unit>>()

    fun fetchAppInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getAppInfo(id)
                .collect { result ->
                    val appInfo = result.getOrNull()
                    if (appInfo != null) {
                        if (appInfo.message != null) {
                            activityState.postValue(LoadingState.LoadingError(appInfo.message))
                        } else if (appInfo.data != null) {
                            // appData for App Info
                            appData = appInfo.data

                            handleAppData()
                            activityState.postValue(LoadingState.LoadingDone)
                        }
                    } else {
                        activityState.postValue(LoadingState.LoadingFailed(LOADING_FAILED))
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    fun handleAppData() {
        appData?.let { data ->
            // get download link params
            appId = data.id
            packageName = data.apkname
            versionCode = data.apkversioncode

            if (data.commentStatusText == "允许评论" || data.entityType == "appForum") {
                tabList = listOf("最近回复", "最新发布", "热度排序")
            } else {
                errMsg = data.commentStatusText
            }
            checkBlock(data.title ?: "") //menuBlock
        }
    }

    fun onGetDownloadLink() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getAppDownloadLink(
                packageName.toString(),
                appId.toString(),
                versionCode.toString()
            )
                .collect { result ->
                    val link = result.getOrNull()
                    if (link != null) {
                        downloadUrl = link
                        download.postValue(Event(true))
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }

    }

    private fun checkBlock(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blockState.postValue(Event((blackListRepo.checkTopic(title))))
        }
    }

    override fun fetchData() {}

    fun checkMenuState() {
        searchState.postValue(Event(Unit))
        appData?.title?.let {
            checkBlock(it)
        }
    }

}

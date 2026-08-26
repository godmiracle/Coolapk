package com.godmiracle.coolapk.ui.base

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.adapter.FooterState
import com.godmiracle.coolapk.adapter.ItemListener
import com.godmiracle.coolapk.logic.model.HomeFeedResponse
import com.godmiracle.coolapk.logic.repository.BlackListRepo
import com.godmiracle.coolapk.logic.repository.HistoryFavoriteRepo
import com.godmiracle.coolapk.logic.repository.NetworkRepo
import com.godmiracle.coolapk.util.Event
import com.godmiracle.coolapk.util.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseAppViewModel(
    val blackListRepo: BlackListRepo,
    val historyRepo: HistoryFavoriteRepo,
    val networkRepo: NetworkRepo
) : BaseViewModel() {

    val dataList = MutableLiveData<List<HomeFeedResponse.Data>>()

    val footerState = MutableLiveData<FooterState>()
    val toastText = MutableLiveData<Event<String?>>()

    open fun showCollection(id: String, title: String) {}

    inner class ItemClickListener : ItemListener {
        override fun onShowCollection(id: String, title: String) {
            showCollection(id, title)
        }

        override fun onViewFeed(
            view: View,
            id: String?,
            uid: String?,
            username: String?,
            userAvatar: String?,
            deviceTitle: String?,
            message: String?,
            dateline: String?,
            rid: Any?,
            isViewReply: Any?
        ) {
            super.onViewFeed(
                view,
                id,
                uid,
                username,
                userAvatar,
                deviceTitle,
                message,
                dateline,
                rid,
                isViewReply
            )
            viewModelScope.launch(Dispatchers.IO) {
                if (!uid.isNullOrEmpty() && PrefManager.isRecordHistory)
                    historyRepo.saveHistory(
                        id.toString(), uid.toString(), username.toString(), userAvatar.toString(),
                        deviceTitle.toString(), message.toString(), dateline.toString()
                    )
            }
        }

        override fun onBlockUser(id: String, uid: String, position: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                blackListRepo.saveUid(uid)
            }
            val currentList = dataList.value?.toMutableList() ?: ArrayList()
            currentList.removeAt(position)
            dataList.postValue(currentList)
        }
    }

    fun saveTopic(title: String) {
        viewModelScope.launch {
            blackListRepo.saveTopic(title)
        }
    }

    fun deleteTopic(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blackListRepo.deleteTopic(title)
        }
    }

}

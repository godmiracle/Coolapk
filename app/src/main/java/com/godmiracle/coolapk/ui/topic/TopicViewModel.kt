package com.godmiracle.coolapk.ui.topic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.adapter.LoadingState
import com.godmiracle.coolapk.constant.Constants
import com.godmiracle.coolapk.logic.model.HomeFeedResponse
import com.godmiracle.coolapk.logic.model.LocalFollowType
import com.godmiracle.coolapk.logic.model.TopicBean
import com.godmiracle.coolapk.logic.repository.BlackListRepo
import com.godmiracle.coolapk.logic.repository.HistoryFavoriteRepo
import com.godmiracle.coolapk.logic.repository.LocalFollowRepo
import com.godmiracle.coolapk.logic.repository.NetworkRepo
import com.godmiracle.coolapk.ui.base.BaseAppViewModel
import com.godmiracle.coolapk.util.Event
import com.godmiracle.coolapk.util.PrefManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TopicViewModel.Factory::class)
class TopicViewModel @AssistedInject constructor(
    @Assisted("url") var url: String,
    @Assisted("title") val title: String,
    @Assisted("id") var id: String,
    @Assisted("type") var type: String,
    blackListRepo: BlackListRepo,
    historyRepo: HistoryFavoriteRepo,
    networkRepo: NetworkRepo,
    private val localFollowRepo: LocalFollowRepo
) : BaseAppViewModel(blackListRepo, historyRepo, networkRepo) {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("url") url: String,
            @Assisted("title") title: String,
            @Assisted("id") id: String,
            @Assisted("type") type: String
        ): TopicViewModel
    }

    var subtitle: String? = null
    var avatar: String? = null
    var topicData: HomeFeedResponse.Data? = null
    var discussionSort: TopicSort = TopicSort.DEFAULT

    var isAInit: Boolean = true
    var postFollowData: HashMap<String, String>? = null
    var isFollow: Boolean = false
    var tabSelected: Int? = null
    var topicList: ArrayList<TopicBean>? = null

    val blockState = MutableLiveData<Event<Boolean>>()
    val followState = MutableLiveData<Event<Boolean>>()

    fun fetchTopicLayout() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getTopicLayout(url)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            activityState.postValue(LoadingState.LoadingError(data.message))
                            return@collect
                        } else if (data.data != null) {
                            topicData = data.data
                            isFollow = data.data.userAction?.follow == 1
                            id = data.data.id ?: ""
                            type = data.data.entityType
                            avatar = data.data.logo
                            subtitle = data.data.intro
                            getTopicList(data.data.tabList, data.data.selectedTab.toString())
                            checkFollow()
                            activityState.postValue(LoadingState.LoadingDone)
                        }
                    } else {
                        activityState.postValue(LoadingState.LoadingFailed(Constants.LOADING_FAILED))
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }


    fun fetchProductLayout() {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getProductLayout(id)
                .collect { result ->
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            activityState.postValue(LoadingState.LoadingError(data.message))
                            return@collect
                        } else if (data.data != null) {
                            topicData = data.data
                            isFollow = data.data.userAction?.follow == 1
                            avatar = data.data.logo
                            subtitle = data.data.intro
                            getTopicList(data.data.tabList, data.data.selectedTab.toString())
                            checkFollow()
                            activityState.postValue(LoadingState.LoadingDone)
                        }
                    } else {
                        activityState.postValue(LoadingState.LoadingFailed(Constants.LOADING_FAILED))
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    private fun getTopicList(tabList: List<HomeFeedResponse.TabList>?, selectedTab: String) {
        tabList?.map {
            TopicBean(it.url.toString(), it.title.toString())
        }?.let {
            topicList = ArrayList()
            topicList?.addAll(it)
        }
        run breaking@{
            tabList?.forEachIndexed { index, tab ->
                if (selectedTab == tab.pageName) {
                    tabSelected = index
                    return@breaking
                }
            }
        }
    }

    fun toggleFollow() {
        val targetFollow = !isFollow
        viewModelScope.launch(Dispatchers.IO) {
            applyFollowState(targetFollow)
        }

        if (!PrefManager.isLogin) {
            toastText.postValue(Event(if (targetFollow) "关注成功" else "取消关注成功"))
            return
        }

        when (type) {
            LocalFollowType.TOPIC -> onGetFollow(
                if (targetFollow) "/v6/feed/followTag" else "/v6/feed/unFollowTag",
                url.replace("/t/", ""),
                null,
                targetFollow
            )

            LocalFollowType.PRODUCT -> {
                if (postFollowData.isNullOrEmpty())
                    postFollowData = HashMap()
                postFollowData?.let { map ->
                    map["id"] = id
                    map["status"] = if (targetFollow) "1" else "0"
                }
                onPostFollow(targetFollow)
            }
        }
    }

    // The local state is authoritative for this screen; server sync is best effort.
    fun onGetFollow(
        followUrl: String,
        tag: String?,
        id: String?,
        requestedFollow: Boolean = !isFollow
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getFollow(followUrl, tag, id)
                .collect { result ->
                    val response = result.getOrNull()
                    if (response != null) {
                        val followed = response.data?.follow?.let { it == 1 }
                            ?: response.message
                                ?.takeIf { it.contains("成功") }
                                ?.let { requestedFollow }
                        if (followed != null) {
                            applyFollowState(followed)
                        }
                        response.message?.let { toastText.postValue(Event(it)) }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
        }
    }

    // follow product
    fun onPostFollow(requestedFollow: Boolean = !isFollow) {
        viewModelScope.launch(Dispatchers.IO) {
            postFollowData?.let {
                networkRepo.postFollow(it)
                    .collect { result ->
                        val response = result.getOrNull()
                        if (response != null) {
                            val followed = response.message
                                ?.takeIf { it.contains("成功") }
                                ?.let { requestedFollow }
                            if (followed != null) {
                                applyFollowState(followed)
                            }
                            response.message?.let { toastText.postValue(Event(it)) }
                        } else {
                            result.exceptionOrNull()?.printStackTrace()
                        }
                    }
            }
        }
    }

    private fun checkTopic(title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            blockState.postValue(Event(blackListRepo.checkTopic(title)))
        }
    }

    private fun checkFollow() {
        viewModelScope.launch(Dispatchers.IO) {
            val localFollowed = localFollowType()?.let { followType ->
                localFollowTargetId()?.let { targetId ->
                    localFollowRepo.isFollowed(followType, targetId)
                }
            } ?: false
            if (localFollowed || !PrefManager.isLogin)
                isFollow = localFollowed
            if (localFollowed && !avatar.isNullOrBlank()) {
                localFollowRepo.updateAvatar(
                    localFollowType().orEmpty(),
                    localFollowTargetId().orEmpty(),
                    avatar.orEmpty()
                )
            }
            followState.postValue(Event(isFollow))
        }
    }

    private fun localFollowType(): String? = when (type) {
        LocalFollowType.TOPIC, LocalFollowType.PRODUCT -> type
        else -> null
    }

    private fun localFollowTargetId(): String? = when (type) {
        LocalFollowType.TOPIC -> url.replace("/t/", "").takeIf { it.isNotBlank() }
        LocalFollowType.PRODUCT -> id.takeIf { it.isNotBlank() }
        else -> null
    }

    private fun localFollowTitle(): String = when (type) {
        LocalFollowType.TOPIC -> url.replace("/t/", "")
        else -> title
    }

    private suspend fun applyFollowState(followed: Boolean) {
        isFollow = followed
        val followType = localFollowType()
        val targetId = localFollowTargetId()
        if (followType != null && targetId != null) {
            if (followed)
                localFollowRepo.save(followType, targetId, localFollowTitle(), avatar.orEmpty())
            else
                localFollowRepo.delete(followType, targetId)
        }
        followState.postValue(Event(followed))
    }

    override fun fetchData() {}

    fun checkMenuState() {
        checkTopic(title)
        checkFollow()
    }

}

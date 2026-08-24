package com.godmiracle.coolapk.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.adapter.FooterState
import com.godmiracle.coolapk.adapter.LoadingState
import com.godmiracle.coolapk.constant.Constants.LOADING_EMPTY
import com.godmiracle.coolapk.constant.Constants.LOADING_END
import com.godmiracle.coolapk.constant.Constants.LOADING_FAILED
import com.godmiracle.coolapk.logic.repository.BlackListRepo
import com.godmiracle.coolapk.logic.repository.HistoryFavoriteRepo
import com.godmiracle.coolapk.logic.repository.NetworkRepo
import com.godmiracle.coolapk.ui.base.BaseAppViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal object TopicSortUrl {
    private val listTypePattern = Regex("([?&])listType=[^&]*")
    private val nestedUrlMarker = Regex("(?i)([?&])url=")
    private val encodedListTypePattern =
        Regex("(?i)(%26|%3F)listType%3D[^%]*(?=%26|$)")

    fun forSort(url: String, sort: TopicSort): String {
        val listType = sort.listType ?: return url
        forEncodedNestedUrl(url, listType)?.let { return it }

        return forRawUrl(url, listType)
    }

    private fun forEncodedNestedUrl(url: String, listType: String): String? {
        val marker = nestedUrlMarker.find(url) ?: return null
        val valueStart = marker.range.last + 1
        val valueEnd = url.indexOf('&', valueStart).let { if (it == -1) url.length else it }
        val encodedValue = url.substring(valueStart, valueEnd)
        if (!encodedValue.contains("%2F", ignoreCase = true)
            && !encodedValue.contains("%3F", ignoreCase = true)
            && !encodedValue.contains("%26", ignoreCase = true)
        )
            return null

        val replacedValue = encodedListTypePattern.replace(encodedValue) {
            "${it.groupValues[1]}listType%3D$listType"
        }
        val sortedValue = if (replacedValue == encodedValue)
            "$encodedValue%26listType%3D$listType"
        else
            replacedValue
        return url.replaceRange(valueStart, valueEnd, sortedValue)
    }

    private fun forRawUrl(url: String, listType: String): String {
        val baseUrl = url
        if (listTypePattern.containsMatchIn(baseUrl))
            return baseUrl.replace(listTypePattern, "\$1listType=$listType")
        val separator = when {
            baseUrl.endsWith('?') || baseUrl.endsWith('&') -> ""
            baseUrl.contains('?') -> "&"
            else -> "?"
        }
        return "$baseUrl${separator}listType=$listType"
    }
}

class TopicContentViewModel @AssistedInject constructor(
    @Assisted("url") var url: String,
    @Assisted("title") var title: String,
    blackListRepo: BlackListRepo,
    historyRepo: HistoryFavoriteRepo,
    networkRepo: NetworkRepo
) : BaseAppViewModel(blackListRepo, historyRepo, networkRepo) {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("url") url: String,
            @Assisted("title") title: String,
        ): TopicContentViewModel
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun provideFactory(assistedFactory: Factory, url: String, title: String)
                : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(url, title) as T
            }
        }
    }

    override fun fetchData() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            networkRepo.getDataList(url, title, "", lastItem, page)
                .onStart {
                    if (isLoadMore) {
                        if (listSize <= 0)
                            loadingState.postValue(LoadingState.Loading)
                        else
                            footerState.postValue(FooterState.Loading)
                    }
                }
                .collect { result ->
                    val dataListList = dataList.value?.toMutableList() ?: ArrayList()
                    val data = result.getOrNull()
                    if (data != null) {
                        if (!data.message.isNullOrEmpty()) {
                            if (listSize <= 0)
                                loadingState.postValue(LoadingState.LoadingError(data.message))
                            else
                                footerState.postValue(FooterState.LoadingError(data.message))
                            return@collect
                        } else if (!data.data.isNullOrEmpty()) {
                            lastItem = data.data.last().id
                            if (isRefreshing)
                                dataListList.clear()
                            if (isRefreshing || isLoadMore) {
                                data.data.forEach {
                                    if (it.entityType in listOf("feed", "topic", "product", "user"))
                                        if (!blackListRepo.checkUid(it.userInfo?.uid.toString())
                                            && !blackListRepo.checkTopic(
                                                it.tags + it.ttitle + it.relationRows?.getOrNull(0)?.title
                                            )
                                        )
                                            dataListList.add(it)
                                }
                            }
                            page++
                            if (listSize <= 0) {
                                loadingState.postValue(LoadingState.LoadingDone)
                            } else
                                footerState.postValue(FooterState.LoadingDone)
                            dataList.postValue(dataListList)
                        } else if (data.data?.isEmpty() == true) {
                            isEnd = true
                            if (listSize <= 0)
                                loadingState.postValue(LoadingState.LoadingFailed(LOADING_EMPTY))
                            else {
                                if (isRefreshing)
                                    dataList.postValue(emptyList())
                                footerState.postValue(FooterState.LoadingEnd(LOADING_END))
                            }
                        }
                    } else {
                        isEnd = true
                        if (listSize <= 0)
                            loadingState.postValue(LoadingState.LoadingFailed(LOADING_FAILED))
                        else
                            footerState.postValue(FooterState.LoadingError(LOADING_FAILED))
                        result.exceptionOrNull()?.printStackTrace()
                    }
                    isRefreshing = false
                    isLoadMore = false
                }
        }
    }

    private val defaultUrl = url
    var sort: TopicSort = TopicSort.DEFAULT
        private set
    private var fetchJob: Job? = null

    fun applySort(nextSort: TopicSort) {
        if (sort == nextSort)
            return

        sort = nextSort
        url = TopicSortUrl.forSort(defaultUrl, nextSort)
        fetchJob?.cancel()
        dataList.value = emptyList()
        listSize = 0
        lastItem = null
        page = 1
        isEnd = false
        isRefreshing = false
        isLoadMore = false
        loadingState.value = LoadingState.Loading
    }


}

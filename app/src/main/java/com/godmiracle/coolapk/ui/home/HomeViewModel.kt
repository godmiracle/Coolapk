package com.godmiracle.coolapk.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godmiracle.coolapk.logic.model.HomeMenu
import com.godmiracle.coolapk.logic.repository.HomeMenuRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeMenuRepo: HomeMenuRepo
) : ViewModel() {

    companion object {
        const val APPLICATION_TAB_TITLE = "应用"
        const val NEWS_TAB_TITLE = "快讯"
        const val COOL_PIC_TAB_TITLE = "酷图"
        const val DEFAULT_TAB_TITLE = "头条"
        private const val TOPIC_TAB_TITLE = "话题"
    }

    var isInit = true
    var position: Int = 0

    val tabListLiveData: LiveData<List<HomeMenu>> = homeMenuRepo.loadAllListLive()
    val restart = MutableLiveData<Boolean>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.delete(APPLICATION_TAB_TITLE)
            homeMenuRepo.delete(COOL_PIC_TAB_TITLE)

            val currentList = homeMenuRepo.loadAllList()
            if (currentList.isNotEmpty() && currentList.none { it.title == NEWS_TAB_TITLE }) {
                val insertPosition = currentList.indexOfFirst { it.title == TOPIC_TAB_TITLE }
                    .takeIf { it >= 0 } ?: currentList.size
                val updatedList = currentList.mapIndexed { index, menu ->
                    menu.copy(position = if (index >= insertPosition) index + 1 else index)
                }
                homeMenuRepo.updateList(updatedList)
                homeMenuRepo.insert(HomeMenu(insertPosition, NEWS_TAB_TITLE, true))
            }
        }
    }

    val defaultList by lazy {
        listOf(
            HomeMenu(0, "关注", true),
            HomeMenu(1, "头条", true),
            HomeMenu(2, "热榜", true),
            HomeMenu(3, NEWS_TAB_TITLE, true),
            HomeMenu(4, "话题", true),
            HomeMenu(5, "数码", true)
        )
    }

    fun initTab() {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.insertList(defaultList)
        }
    }

    fun updateTab(menuList: List<HomeMenu>) {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.updateList(menuList)
            restart.postValue(true)
        }
    }

}

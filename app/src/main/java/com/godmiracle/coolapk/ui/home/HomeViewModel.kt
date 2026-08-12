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
    }

    var isInit = true
    var position: Int = 0

    val tabListLiveData: LiveData<List<HomeMenu>> = homeMenuRepo.loadAllListLive()
    val restart = MutableLiveData<Boolean>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            homeMenuRepo.delete(APPLICATION_TAB_TITLE)
        }
    }

    val defaultList by lazy {
        listOf(
            HomeMenu(0, "关注", true),
            HomeMenu(1, "头条", true),
            HomeMenu(2, "热榜", true),
            HomeMenu(3, "话题", true),
            HomeMenu(4, "数码", true),
            HomeMenu(5, "酷图", true)
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

package com.godmiracle.coolapk.logic.repository

import androidx.lifecycle.LiveData
import com.godmiracle.coolapk.logic.dao.HomeMenuDao
import com.godmiracle.coolapk.logic.model.HomeMenu
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeMenuRepo @Inject constructor(
    private val homeMenuDao: HomeMenuDao,
) {

    fun loadAllListLive(): LiveData<List<HomeMenu>> {
        return homeMenuDao.loadAllListLive()
    }

    suspend fun loadAllList(): List<HomeMenu> {
        return homeMenuDao.loadAllList()
    }

    suspend fun insert(homeMenu: HomeMenu) {
        homeMenuDao.insert(homeMenu)
    }

    suspend fun insertList(list: List<HomeMenu>) {
        homeMenuDao.insertList(list)
    }

    suspend fun updateList(list: List<HomeMenu>) {
        homeMenuDao.updateList(list)
    }

    suspend fun delete(title: String) {
        homeMenuDao.delete(title)
    }

    suspend fun deleteAll() {
        homeMenuDao.deleteAll()
    }

}

package com.godmiracle.coolapk.logic.repository

import androidx.lifecycle.LiveData
import com.godmiracle.coolapk.di.SearchHistory
import com.godmiracle.coolapk.logic.dao.StringEntityDao
import com.godmiracle.coolapk.logic.model.StringEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepo @Inject constructor(
    @SearchHistory
    private val searchHistoryDao: StringEntityDao,
) {

    fun loadAllListLive(): LiveData<List<StringEntity>> {
        return searchHistoryDao.loadAllListLive()
    }

    suspend fun insertHistory(history: StringEntity) {
        searchHistoryDao.insert(history)
    }

    suspend fun insertList(list: List<StringEntity>) {
        searchHistoryDao.insertList(list)
    }

    suspend fun saveHistory(history: String) {
        if (!searchHistoryDao.isExist(history)) {
            searchHistoryDao.insert(StringEntity(history))
        }
    }

    suspend fun deleteHistory(history: String) {
        searchHistoryDao.delete(history)
    }

    suspend fun deleteAllHistory() {
        searchHistoryDao.deleteAll()
    }

    suspend fun checkHistory(history: String): Boolean {
        return searchHistoryDao.isExist(history)
    }

    suspend fun updateHistory(data: String, newId: Long) {
        searchHistoryDao.updateHistory(data, newId)
    }

}
package com.godmiracle.coolapk.logic.repository

import androidx.lifecycle.LiveData
import com.godmiracle.coolapk.logic.dao.LocalFollowDao
import com.godmiracle.coolapk.logic.model.LocalFollow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFollowRepo @Inject constructor(
    private val dao: LocalFollowDao
) {

    fun observeAll(): LiveData<List<LocalFollow>> = dao.observeAll()

    suspend fun isFollowed(type: String, targetId: String): Boolean =
        dao.isFollowed(type, targetId)

    suspend fun save(type: String, targetId: String, title: String, avatar: String = "") {
        dao.insert(LocalFollow(type, targetId, title, avatar))
    }

    suspend fun updateAvatar(type: String, targetId: String, avatar: String) {
        dao.updateAvatar(type, targetId, avatar)
    }

    suspend fun delete(type: String, targetId: String) {
        dao.delete(type, targetId)
    }
}

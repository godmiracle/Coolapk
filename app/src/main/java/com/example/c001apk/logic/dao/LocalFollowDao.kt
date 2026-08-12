package com.example.c001apk.logic.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.c001apk.logic.model.LocalFollow

@Dao
interface LocalFollowDao {

    @Query("SELECT * FROM local_follow ORDER BY updatedAt DESC")
    fun observeAll(): LiveData<List<LocalFollow>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM local_follow WHERE type = :type AND targetId = :targetId)"
    )
    suspend fun isFollowed(type: String, targetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(follow: LocalFollow)

    @Query(
        "UPDATE local_follow SET avatar = :avatar " +
            "WHERE type = :type AND targetId = :targetId"
    )
    suspend fun updateAvatar(type: String, targetId: String, avatar: String)

    @Query("DELETE FROM local_follow WHERE type = :type AND targetId = :targetId")
    suspend fun delete(type: String, targetId: String)
}

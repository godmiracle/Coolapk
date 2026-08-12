package com.godmiracle.coolapk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.godmiracle.coolapk.logic.dao.RecentAtUserDao
import com.godmiracle.coolapk.logic.model.RecentAtUser

@Database(version = 2, entities = [RecentAtUser::class])
abstract class RecentAtUserDatabase : RoomDatabase() {
    abstract fun recentAtUserDao(): RecentAtUserDao
}
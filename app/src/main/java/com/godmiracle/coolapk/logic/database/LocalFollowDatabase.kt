package com.godmiracle.coolapk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.godmiracle.coolapk.logic.dao.LocalFollowDao
import com.godmiracle.coolapk.logic.model.LocalFollow

@Database(version = 2, entities = [LocalFollow::class])
abstract class LocalFollowDatabase : RoomDatabase() {
    abstract fun localFollowDao(): LocalFollowDao
}

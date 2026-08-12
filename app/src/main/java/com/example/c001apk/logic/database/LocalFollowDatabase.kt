package com.example.c001apk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.c001apk.logic.dao.LocalFollowDao
import com.example.c001apk.logic.model.LocalFollow

@Database(version = 2, entities = [LocalFollow::class])
abstract class LocalFollowDatabase : RoomDatabase() {
    abstract fun localFollowDao(): LocalFollowDao
}

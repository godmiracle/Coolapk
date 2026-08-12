package com.godmiracle.coolapk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.godmiracle.coolapk.logic.dao.HomeMenuDao
import com.godmiracle.coolapk.logic.model.HomeMenu

@Database(version = 5, entities = [HomeMenu::class])
abstract class HomeMenuDatabase : RoomDatabase() {
    abstract fun homeMenuDao(): HomeMenuDao
}
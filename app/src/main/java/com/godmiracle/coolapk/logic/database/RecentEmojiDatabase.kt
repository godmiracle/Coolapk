package com.godmiracle.coolapk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.godmiracle.coolapk.logic.dao.StringEntityDao
import com.godmiracle.coolapk.logic.model.StringEntity

@Database(version = 2, entities = [StringEntity::class])
abstract class RecentEmojiDatabase : RoomDatabase() {
    abstract fun recentEmojiDao(): StringEntityDao
}
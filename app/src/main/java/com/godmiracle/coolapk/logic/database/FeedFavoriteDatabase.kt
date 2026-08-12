package com.godmiracle.coolapk.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.godmiracle.coolapk.logic.dao.HistoryFavoriteDao
import com.godmiracle.coolapk.logic.model.FeedEntity


@Database(version = 2, entities = [FeedEntity::class])
abstract class FeedFavoriteDatabase : RoomDatabase() {
    abstract fun feedFavoriteDao(): HistoryFavoriteDao
}
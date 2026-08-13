package com.godmiracle.coolapk.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.godmiracle.coolapk.logic.dao.HistoryFavoriteDao
import com.godmiracle.coolapk.logic.dao.HomeMenuDao
import com.godmiracle.coolapk.logic.dao.LocalFollowDao
import com.godmiracle.coolapk.logic.dao.RecentAtUserDao
import com.godmiracle.coolapk.logic.dao.StringEntityDao
import com.godmiracle.coolapk.logic.database.BrowseHistoryDatabase
import com.godmiracle.coolapk.logic.database.FeedFavoriteDatabase
import com.godmiracle.coolapk.logic.database.HomeMenuDatabase
import com.godmiracle.coolapk.logic.database.LocalFollowDatabase
import com.godmiracle.coolapk.logic.database.RecentAtUserDatabase
import com.godmiracle.coolapk.logic.database.RecentEmojiDatabase
import com.godmiracle.coolapk.logic.database.SearchHistoryDatabase
import com.godmiracle.coolapk.logic.database.TopicBlackListDatabase
import com.godmiracle.coolapk.logic.database.UserBlackListDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserBlackList

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TopicBlackList

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchHistory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecentEmoji

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BrowseHistory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FeedFavorite

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @RecentEmoji
    @Singleton
    @Provides
    fun provideRecentEmojiDao(stringEntityDatabase: RecentEmojiDatabase): StringEntityDao {
        return stringEntityDatabase.recentEmojiDao()
    }

    @Singleton
    @Provides
    fun provideRecentEmojiDatabase(@ApplicationContext context: Context): RecentEmojiDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RecentEmojiDatabase::class.java, "recent_emoji.db"
        )
            .addMigrations(StringEntityDatabase_MIGRATION_1_2)
            .build()
    }

    @UserBlackList
    @Singleton
    @Provides
    fun provideUserBlackListDao(stringEntityDatabase: UserBlackListDatabase): StringEntityDao {
        return stringEntityDatabase.userBlackListDao()
    }

    @Singleton
    @Provides
    fun provideUserBlackListDatabase(@ApplicationContext context: Context): UserBlackListDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            UserBlackListDatabase::class.java, "user_blacklist.db"
        )
            .addMigrations(StringEntityDatabase_MIGRATION_1_2)
            .build()
    }

    @TopicBlackList
    @Singleton
    @Provides
    fun provideTopicBlackListDao(stringEntityDatabase: TopicBlackListDatabase): StringEntityDao {
        return stringEntityDatabase.topicBlackListDao()
    }

    @Singleton
    @Provides
    fun provideTopicBlackListDatabase(@ApplicationContext context: Context): TopicBlackListDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TopicBlackListDatabase::class.java, "topic_blacklist.db"
        )
            .addMigrations(StringEntityDatabase_MIGRATION_1_2)
            .build()
    }

    @SearchHistory
    @Singleton
    @Provides
    fun provideSearchHistoryDao(stringEntityDatabase: SearchHistoryDatabase): StringEntityDao {
        return stringEntityDatabase.searchHistoryDao()
    }

    @Singleton
    @Provides
    fun provideSearchHistoryDatabase(@ApplicationContext context: Context): SearchHistoryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            SearchHistoryDatabase::class.java, "search_history.db"
        )
            .addMigrations(StringEntityDatabase_MIGRATION_1_2)
            .build()
    }

    @BrowseHistory
    @Singleton
    @Provides
    fun provideBrowseHistoryDao(browseHistoryDatabase: BrowseHistoryDatabase): HistoryFavoriteDao {
        return browseHistoryDatabase.browseHistoryDao()
    }

    @Singleton
    @Provides
    fun provideBrowseHistoryDatabase(@ApplicationContext context: Context): BrowseHistoryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BrowseHistoryDatabase::class.java, "browse_history.db"
        ).build()
    }

    @FeedFavorite
    @Singleton
    @Provides
    fun provideFeedFavoriteDao(feedFavoriteDatabase: FeedFavoriteDatabase): HistoryFavoriteDao {
        return feedFavoriteDatabase.feedFavoriteDao()
    }

    @Singleton
    @Provides
    fun provideFeedFavoriteDatabase(@ApplicationContext context: Context): FeedFavoriteDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            FeedFavoriteDatabase::class.java, "feed_favorite.db"
        )
            .addMigrations(FeedFavoriteDatabase_MIGRATION_1_3)
            .addMigrations(FeedFavoriteDatabase_MIGRATION_2_3)
            .build()
    }

    @Singleton
    @Provides
    fun provideLocalFollowDao(database: LocalFollowDatabase): LocalFollowDao {
        return database.localFollowDao()
    }

    @Singleton
    @Provides
    fun provideLocalFollowDatabase(@ApplicationContext context: Context): LocalFollowDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            LocalFollowDatabase::class.java, "local_follow.db"
        ).addMigrations(LocalFollowDatabase_MIGRATION_1_2).build()
    }

    @Singleton
    @Provides
    fun provideHomeMenuDao(homeMenuDatabase: HomeMenuDatabase): HomeMenuDao {
        return homeMenuDatabase.homeMenuDao()
    }

    @Singleton
    @Provides
    fun provideHomeMenuDatabase(@ApplicationContext context: Context): HomeMenuDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HomeMenuDatabase::class.java, "home_menu.db"
        )
            .addMigrations(HomeMenuDatabase_MIGRATION_1_2)
            .addMigrations(HomeMenuDatabase_MIGRATION_2_3)
            .addMigrations(HomeMenuDatabase_MIGRATION_3_4)
            .addMigrations(HomeMenuDatabase_MIGRATION_4_5)
            .build()
    }

    @Singleton
    @Provides
    fun provideRecentAtUserDao(recentAtUserDatabase: RecentAtUserDatabase): RecentAtUserDao {
        return recentAtUserDatabase.recentAtUserDao()
    }

    @Singleton
    @Provides
    fun provideRecentAtUserDatabase(@ApplicationContext context: Context): RecentAtUserDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RecentAtUserDatabase::class.java, "recent_at_user.db"
        )
            .addMigrations(RecentAtUserDatabase_MIGRATION_1_2)
            .build()
    }

}

object LocalFollowDatabase_MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE local_follow ADD COLUMN avatar TEXT NOT NULL DEFAULT ''")
    }
}

private const val FEED_ENTITY_CREATE_SQL = """
    CREATE TABLE IF NOT EXISTS `FeedEntity_new` (
        `fid` TEXT NOT NULL,
        `uid` TEXT NOT NULL,
        `uname` TEXT NOT NULL,
        `avatar` TEXT NOT NULL,
        `device` TEXT NOT NULL,
        `message` TEXT NOT NULL,
        `pubDate` TEXT NOT NULL,
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
    )
"""

private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean {
    return query(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
        arrayOf(tableName)
    ).use { cursor ->
        cursor.moveToFirst()
    }
}

object FeedFavoriteDatabase_MIGRATION_1_3 : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(FEED_ENTITY_CREATE_SQL)
        if (db.hasTable("FeedFavorite")) {
            db.execSQL(
                """
                INSERT INTO `FeedEntity_new`
                    (`fid`, `uid`, `uname`, `avatar`, `device`, `message`, `pubDate`, `id`)
                SELECT `feedId`, '', '', '', '', '', '', `id`
                FROM `FeedFavorite`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `FeedFavorite`")
        }
        db.execSQL("ALTER TABLE `FeedEntity_new` RENAME TO `FeedEntity`")
    }
}

object FeedFavoriteDatabase_MIGRATION_2_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (db.hasTable("FeedEntity")) return

        db.execSQL(FEED_ENTITY_CREATE_SQL)
        if (db.hasTable("FeedFavorite")) {
            db.execSQL(
                """
                INSERT INTO `FeedEntity_new`
                    (`fid`, `uid`, `uname`, `avatar`, `device`, `message`, `pubDate`, `id`)
                SELECT `feedId`, `uid`, `uname`, `avatar`, `device`, `message`, `pubDate`, `id`
                FROM `FeedFavorite`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `FeedFavorite`")
        }
        db.execSQL("ALTER TABLE `FeedEntity_new` RENAME TO `FeedEntity`")
    }
}

object HomeMenuDatabase_MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("insert into HomeMenu (title,isEnable) values ('数码',1)")
    }
}

object HomeMenuDatabase_MIGRATION_2_3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `HomeMenu_new` (
                `position` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `isEnable` INTEGER NOT NULL,
                PRIMARY KEY(`position`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `HomeMenu_new` (`position`, `title`, `isEnable`)
            SELECT (
                SELECT COUNT(*)
                FROM `HomeMenu` AS `earlier_menu`
                WHERE `earlier_menu`.`id` < `source_menu`.`id`
            ), `source_menu`.`title`, `source_menu`.`isEnable`
            FROM `HomeMenu` AS `source_menu`
            ORDER BY `source_menu`.`id`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `HomeMenu`")
        db.execSQL("ALTER TABLE `HomeMenu_new` RENAME TO `HomeMenu`")
    }
}

object HomeMenuDatabase_MIGRATION_3_4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO `HomeMenu` (`position`, `title`, `isEnable`)
            SELECT COALESCE((SELECT MAX(`position`) + 1 FROM `HomeMenu`), 0), '酷图', 1
            WHERE NOT EXISTS (SELECT 1 FROM `HomeMenu` WHERE `title` = '酷图')
            """.trimIndent()
        )
    }
}

object HomeMenuDatabase_MIGRATION_4_5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `HomeMenu_new` (`position` INTEGER NOT NULL, `title` TEXT NOT NULL, `isEnable` INTEGER NOT NULL, PRIMARY KEY(`title`))")
        db.execSQL("INSERT INTO HomeMenu_new (position, title, isEnable) SELECT position, title, isEnable FROM HomeMenu")
        db.execSQL("DROP TABLE HomeMenu")
        db.execSQL("ALTER TABLE HomeMenu_new RENAME TO HomeMenu")
    }
}

object RecentAtUserDatabase_MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `RecentAtUser_new` (
                `id` INTEGER NOT NULL,
                `group` TEXT NOT NULL,
                `avatar` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                PRIMARY KEY(`username`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT OR IGNORE INTO `RecentAtUser_new` (`id`, `group`, `avatar`, `username`)
            SELECT `id`, `group`, `avatar`, `username`
            FROM `RecentAtUser` ORDER BY `id`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `RecentAtUser`")
        db.execSQL("ALTER TABLE `RecentAtUser_new` RENAME TO `RecentAtUser`")
    }
}

object StringEntityDatabase_MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `StringEntity_new` (`id` INTEGER NOT NULL, `data` TEXT NOT NULL, PRIMARY KEY(`data`))")
        db.execSQL(
            """
            INSERT INTO `StringEntity_new` (`id`, `data`)
            SELECT MAX(`id`), `data`
            FROM `StringEntity`
            GROUP BY `data`
            ORDER BY MAX(`id`), `data`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE StringEntity")
        db.execSQL("ALTER TABLE StringEntity_new RENAME TO StringEntity")
    }
}

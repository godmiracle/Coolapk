package com.godmiracle.coolapk.logic.database

import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.godmiracle.coolapk.di.FeedFavoriteDatabase_MIGRATION_1_3
import com.godmiracle.coolapk.di.FeedFavoriteDatabase_MIGRATION_2_3
import com.godmiracle.coolapk.di.HomeMenuDatabase_MIGRATION_1_2
import com.godmiracle.coolapk.di.HomeMenuDatabase_MIGRATION_2_3
import com.godmiracle.coolapk.di.HomeMenuDatabase_MIGRATION_3_4
import com.godmiracle.coolapk.di.HomeMenuDatabase_MIGRATION_4_5
import com.godmiracle.coolapk.di.LocalFollowDatabase_MIGRATION_1_2
import com.godmiracle.coolapk.di.RecentAtUserDatabase_MIGRATION_1_2
import com.godmiracle.coolapk.di.StringEntityDatabase_MIGRATION_1_2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val feedHelper = migrationHelper(FeedFavoriteDatabase::class.java)
    private val feedLegacyV2Helper = migrationHelper(FEED_LEGACY_V2_FIXTURE)
    private val homeHelper = migrationHelper(HomeMenuDatabase::class.java)
    private val localFollowHelper = migrationHelper(LocalFollowDatabase::class.java)
    private val recentAtUserHelper = migrationHelper(RecentAtUserDatabase::class.java)

    @Before
    fun cleanDatabases() {
        TEST_DATABASES.forEach { context.deleteDatabase(it) }
    }

    @Test
    fun feedFavoriteV1ToCurrentPreservesLegacyId() {
        feedHelper.createDatabase(FEED_DATABASE_V1, 1).apply {
            execSQL("INSERT INTO FeedFavorite (feedId, id) VALUES ('1001', 7)")
            close()
        }

        feedHelper.runMigrationsAndValidate(
            FEED_DATABASE_V1,
            3,
            true,
            FeedFavoriteDatabase_MIGRATION_1_3
        ).use { db ->
            db.assertIdentityHash(FEED_IDENTITY_HASH)
            db.query(
                """
                SELECT fid, id, uid, uname, avatar, device, message, pubDate
                FROM FeedEntity
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("1001", cursor.getString(0))
                assertEquals(7L, cursor.getLong(1))
                for (column in 2..7) {
                    assertEquals("", cursor.getString(column))
                }
            }
        }
    }

    @Test
    fun feedFavoriteLegacyV2TableToCurrentPreservesContent() {
        feedLegacyV2Helper.createDatabase(FEED_DATABASE_V2_LEGACY, 2).apply {
            execSQL(
                """
                INSERT INTO FeedFavorite
                    (uid, uname, feedId, avatar, id, message, device, pubDate)
                VALUES ('u1', 'name', '1002', 'avatar', 8, 'message', 'device', 'date')
                """.trimIndent()
            )
            close()
        }

        feedLegacyV2Helper.runMigrationsAndValidate(
            FEED_DATABASE_V2_LEGACY,
            3,
            true,
            FeedFavoriteDatabase_MIGRATION_2_3
        ).use { db ->
            db.assertIdentityHash(FEED_IDENTITY_HASH)
            db.query(
                """
                SELECT fid, uid, uname, avatar, id, message, device, pubDate
                FROM FeedEntity
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("1002", cursor.getString(0))
                assertEquals("u1", cursor.getString(1))
                assertEquals("name", cursor.getString(2))
                assertEquals("avatar", cursor.getString(3))
                assertEquals(8L, cursor.getLong(4))
                assertEquals("message", cursor.getString(5))
                assertEquals("device", cursor.getString(6))
                assertEquals("date", cursor.getString(7))
            }
        }
    }

    @Test
    fun feedFavoriteCurrentV2TableToCurrentPreservesContent() {
        feedHelper.createDatabase(FEED_DATABASE_V2_CURRENT, 2).apply {
            execSQL(
                """
                INSERT INTO FeedEntity
                    (fid, uid, uname, avatar, device, message, pubDate, id)
                VALUES ('1003', 'u2', 'name2', 'avatar2', 'device2', 'message2', 'date2', 9)
                """.trimIndent()
            )
            close()
        }

        feedHelper.runMigrationsAndValidate(
            FEED_DATABASE_V2_CURRENT,
            3,
            true,
            FeedFavoriteDatabase_MIGRATION_2_3
        ).use { db ->
            db.assertIdentityHash(FEED_IDENTITY_HASH)
            db.query(
                """
                SELECT fid, uid, uname, avatar, device, message, pubDate, id
                FROM FeedEntity
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("1003", cursor.getString(0))
                assertEquals("u2", cursor.getString(1))
                assertEquals("name2", cursor.getString(2))
                assertEquals("avatar2", cursor.getString(3))
                assertEquals("device2", cursor.getString(4))
                assertEquals("message2", cursor.getString(5))
                assertEquals("date2", cursor.getString(6))
                assertEquals(9L, cursor.getLong(7))
            }
        }
    }

    @Test
    fun homeMenuV1ToCurrentPreservesRealDefaultOrderAndState() {
        homeHelper.createDatabase(HOME_DATABASE_V1, 1).apply {
            insertRealHomeMenuV1Rows()
            execSQL("UPDATE HomeMenu SET isEnable = 0 WHERE title = '热榜'")
            close()
        }

        homeHelper.runMigrationsAndValidate(
            HOME_DATABASE_V1,
            5,
            true,
            HomeMenuDatabase_MIGRATION_1_2,
            HomeMenuDatabase_MIGRATION_2_3,
            HomeMenuDatabase_MIGRATION_3_4,
            HomeMenuDatabase_MIGRATION_4_5
        ).use { db ->
            db.assertIdentityHash(HOME_MENU_IDENTITY_HASH)
            assertEquals(EXPECTED_HOME_MENU_ROWS, db.readHomeMenuRows())
        }
    }

    @Test
    fun homeMenuV2ToCurrentPreservesRealV1RowsAndDigitalAddition() {
        homeHelper.createDatabase(HOME_DATABASE_V2, 2).apply {
            insertRealHomeMenuV1Rows()
            execSQL("INSERT INTO HomeMenu (title, isEnable, id) VALUES ('数码', 1, 6)")
            execSQL("UPDATE HomeMenu SET isEnable = 0 WHERE title = '热榜'")
            close()
        }

        homeHelper.runMigrationsAndValidate(
            HOME_DATABASE_V2,
            5,
            true,
            HomeMenuDatabase_MIGRATION_2_3,
            HomeMenuDatabase_MIGRATION_3_4,
            HomeMenuDatabase_MIGRATION_4_5
        ).use { db ->
            db.assertIdentityHash(HOME_MENU_IDENTITY_HASH)
            assertEquals(EXPECTED_HOME_MENU_ROWS, db.readHomeMenuRows())
        }
    }

    @Test
    fun recentAtUserV1ToCurrentPreservesUniqueUsersDeterministically() {
        recentAtUserHelper.createDatabase(RECENT_AT_USER_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO RecentAtUser (`group`, avatar, username, id)
                VALUES ('recent', 'a1', 'user', 1)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO RecentAtUser (`group`, avatar, username, id)
                VALUES ('follow', 'a2', 'user', 2)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO RecentAtUser (`group`, avatar, username, id)
                VALUES ('recent', 'a3', 'other', 3)
                """.trimIndent()
            )
            close()
        }

        recentAtUserHelper.runMigrationsAndValidate(
            RECENT_AT_USER_DATABASE,
            2,
            true,
            RecentAtUserDatabase_MIGRATION_1_2
        ).use { db ->
            db.assertIdentityHash(RECENT_AT_USER_IDENTITY_HASH)
            assertEquals(
                listOf(
                    RecentAtUserRow(1, "recent", "a1", "user"),
                    RecentAtUserRow(3, "recent", "a3", "other")
                ),
                db.readRecentAtUserRows()
            )
        }
    }

    @Test
    fun localFollowV1ToCurrentPreservesRowsAndDefaultsAvatar() {
        localFollowHelper.createDatabase(LOCAL_FOLLOW_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO local_follow (type, targetId, title, updatedAt)
                VALUES ('topic', 't1', 'Android', 100)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO local_follow (type, targetId, title, updatedAt)
                VALUES ('product', 'p1', 'Phone', 200)
                """.trimIndent()
            )
            close()
        }

        localFollowHelper.runMigrationsAndValidate(
            LOCAL_FOLLOW_DATABASE,
            2,
            true,
            LocalFollowDatabase_MIGRATION_1_2
        ).use { db ->
            db.assertIdentityHash(LOCAL_FOLLOW_IDENTITY_HASH)
            assertEquals(
                listOf(
                    LocalFollowRow("product", "p1", "Phone", "", 200),
                    LocalFollowRow("topic", "t1", "Android", "", 100)
                ),
                db.readLocalFollowRows()
            )
        }
    }

    @Test
    fun recentEmojiV1ToCurrentKeepsLatestDuplicate() {
        assertStringEntityMigration(
            RecentEmojiDatabase::class.java,
            RECENT_EMOJI_DATABASE
        )
    }

    @Test
    fun searchHistoryV1ToCurrentKeepsLatestDuplicate() {
        assertStringEntityMigration(
            SearchHistoryDatabase::class.java,
            SEARCH_HISTORY_DATABASE
        )
    }

    @Test
    fun topicBlackListV1ToCurrentKeepsLatestDuplicate() {
        assertStringEntityMigration(
            TopicBlackListDatabase::class.java,
            TOPIC_BLACKLIST_DATABASE
        )
    }

    @Test
    fun userBlackListV1ToCurrentKeepsLatestDuplicate() {
        assertStringEntityMigration(
            UserBlackListDatabase::class.java,
            USER_BLACKLIST_DATABASE
        )
    }

    private fun assertStringEntityMigration(
        databaseClass: Class<out RoomDatabase>,
        databaseName: String
    ) {
        val helper = migrationHelper(databaseClass)
        helper.createDatabase(databaseName, 1).apply {
            execSQL("INSERT INTO StringEntity (data, id) VALUES ('duplicate', 10)")
            execSQL("INSERT INTO StringEntity (data, id) VALUES ('unique-old', 20)")
            execSQL("INSERT INTO StringEntity (data, id) VALUES ('unique-new', 25)")
            execSQL("INSERT INTO StringEntity (data, id) VALUES ('duplicate', 30)")
            close()
        }

        helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            StringEntityDatabase_MIGRATION_1_2
        ).use { db ->
            db.assertIdentityHash(STRING_ENTITY_IDENTITY_HASH)
            assertEquals(
                listOf(
                    StringEntityRow("duplicate", 30),
                    StringEntityRow("unique-new", 25),
                    StringEntityRow("unique-old", 20)
                ),
                db.readStringEntityRows()
            )
        }
    }

    private fun migrationHelper(
        databaseClass: Class<out RoomDatabase>
    ): MigrationTestHelper {
        return migrationHelper(requireNotNull(databaseClass.canonicalName))
    }

    // Keep fixtures self-contained instead of resolving KSP output from app/schemas.
    @Suppress("DEPRECATION")
    private fun migrationHelper(fixtureFolder: String): MigrationTestHelper {
        return MigrationTestHelper(
            instrumentation,
            MIGRATION_FIXTURE_ROOT + "/" + fixtureFolder,
            FrameworkSQLiteOpenHelperFactory()
        )
    }

    private fun SupportSQLiteDatabase.insertRealHomeMenuV1Rows() {
        execSQL(
            """
            INSERT INTO HomeMenu (title, isEnable, id) VALUES
                ('关注', 1, 1),
                ('应用', 1, 2),
                ('头条', 1, 3),
                ('热榜', 1, 4),
                ('话题', 1, 5)
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.assertIdentityHash(expected: String) {
        query(
            "SELECT identity_hash FROM room_master_table WHERE id = 42"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(expected, cursor.getString(0))
        }
    }

    private fun SupportSQLiteDatabase.readHomeMenuRows(): List<HomeMenuRow> {
        return query(
            "SELECT position, title, isEnable FROM HomeMenu ORDER BY position"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        HomeMenuRow(
                            cursor.getInt(0),
                            cursor.getString(1),
                            cursor.getInt(2)
                        )
                    )
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.readRecentAtUserRows(): List<RecentAtUserRow> {
        return query(
            """
            SELECT id, `group`, avatar, username
            FROM RecentAtUser
            ORDER BY id
            """.trimIndent()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        RecentAtUserRow(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3)
                        )
                    )
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.readLocalFollowRows(): List<LocalFollowRow> {
        return query(
            """
            SELECT type, targetId, title, avatar, updatedAt
            FROM local_follow
            ORDER BY type, targetId
            """.trimIndent()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        LocalFollowRow(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getLong(4)
                        )
                    )
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.readStringEntityRows(): List<StringEntityRow> {
        return query(
            "SELECT data, id FROM StringEntity ORDER BY id DESC, data"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(StringEntityRow(cursor.getString(0), cursor.getLong(1)))
                }
            }
        }
    }

    private data class HomeMenuRow(
        val position: Int,
        val title: String,
        val isEnable: Int
    )

    private data class RecentAtUserRow(
        val id: Long,
        val group: String,
        val avatar: String,
        val username: String
    )

    private data class LocalFollowRow(
        val type: String,
        val targetId: String,
        val title: String,
        val avatar: String,
        val updatedAt: Long
    )

    private data class StringEntityRow(
        val data: String,
        val id: Long
    )

    companion object {
        private const val MIGRATION_FIXTURE_ROOT = "room-migration-fixtures"
        private const val FEED_LEGACY_V2_FIXTURE =
            "com.godmiracle.coolapk.logic.database.FeedFavoriteDatabaseLegacyV2"

        private const val FEED_DATABASE_V1 = "feed_favorite_migration_v1.db"
        private const val FEED_DATABASE_V2_LEGACY =
            "feed_favorite_migration_v2_legacy.db"
        private const val FEED_DATABASE_V2_CURRENT =
            "feed_favorite_migration_v2_current.db"
        private const val HOME_DATABASE_V1 = "home_menu_migration_v1.db"
        private const val HOME_DATABASE_V2 = "home_menu_migration_v2.db"
        private const val RECENT_AT_USER_DATABASE =
            "recent_at_user_migration_v1.db"
        private const val LOCAL_FOLLOW_DATABASE = "local_follow_migration_v1.db"
        private const val RECENT_EMOJI_DATABASE = "recent_emoji_migration_v1.db"
        private const val SEARCH_HISTORY_DATABASE = "search_history_migration_v1.db"
        private const val TOPIC_BLACKLIST_DATABASE =
            "topic_blacklist_migration_v1.db"
        private const val USER_BLACKLIST_DATABASE =
            "user_blacklist_migration_v1.db"

        private const val FEED_IDENTITY_HASH =
            "b1e18544632d0a39cd006f946c2e2b6b"
        private const val HOME_MENU_IDENTITY_HASH =
            "b0bad9ad3a4f15dcc4d2894038da96e3"
        private const val RECENT_AT_USER_IDENTITY_HASH =
            "6b167c4c8a07d14e6d8cb02211ea50cd"
        private const val LOCAL_FOLLOW_IDENTITY_HASH =
            "ae73fee3fbd6fc88031b124285c1d442"
        private const val STRING_ENTITY_IDENTITY_HASH =
            "e9222a72bd4f0a2dde1f1be0b6453dba"

        private val EXPECTED_HOME_MENU_ROWS = listOf(
            HomeMenuRow(0, "关注", 1),
            HomeMenuRow(1, "应用", 1),
            HomeMenuRow(2, "头条", 1),
            HomeMenuRow(3, "热榜", 0),
            HomeMenuRow(4, "话题", 1),
            HomeMenuRow(5, "数码", 1),
            HomeMenuRow(6, "酷图", 1)
        )

        private val TEST_DATABASES = listOf(
            FEED_DATABASE_V1,
            FEED_DATABASE_V2_LEGACY,
            FEED_DATABASE_V2_CURRENT,
            HOME_DATABASE_V1,
            HOME_DATABASE_V2,
            RECENT_AT_USER_DATABASE,
            LOCAL_FOLLOW_DATABASE,
            RECENT_EMOJI_DATABASE,
            SEARCH_HISTORY_DATABASE,
            TOPIC_BLACKLIST_DATABASE,
            USER_BLACKLIST_DATABASE
        )
    }
}

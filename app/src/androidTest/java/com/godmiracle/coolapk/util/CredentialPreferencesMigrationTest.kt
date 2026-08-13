package com.godmiracle.coolapk.util

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.godmiracle.coolapk.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import java.util.ArrayDeque

@RunWith(AndroidJUnit4::class)
class CredentialPreferencesMigrationTest {

    private lateinit var legacyPreferences: SharedPreferences
    private lateinit var credentialPreferences: SharedPreferences

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        legacyPreferences = context.getSharedPreferences("credential_migration_legacy", MODE_PRIVATE)
        credentialPreferences = context.getSharedPreferences("credential_migration_credentials", MODE_PRIVATE)
        clearPreferences()
    }

    @After
    fun tearDown() {
        clearPreferences()
    }

    @Test
    fun migratesEveryCredentialKeyAndCleansLegacyValues() {
        seedLegacyCredentials()
        assertTrue(legacyPreferences.edit().putInt("dark_theme", 2).commit())

        assertTrue(CredentialPreferencesMigration.migrate(legacyPreferences, credentialPreferences))

        assertMigratedCredentials()
        assertTrue("Ordinary settings must remain in settings.xml", legacyPreferences.contains("dark_theme"))
        assertFalse(CredentialPreferencesMigration.migrate(legacyPreferences, credentialPreferences))
        assertMigratedCredentials()
    }

    @Test
    fun retriesSafelyAfterEachCommitPhaseFails() {
        (1..3).forEach { failedCommit ->
            clearPreferences()
            seedLegacyCredentials()
            var commitCount = 0

            assertFalse(
                CredentialPreferencesMigration.migrate(
                    legacyPreferences = legacyPreferences,
                    credentialPreferences = credentialPreferences,
                    commitEditor = { editor ->
                        commitCount += 1
                        if (commitCount == failedCommit) false else editor.commit()
                    },
                )
            )
            assertEquals(failedCommit, commitCount)

            when (failedCommit) {
                1 -> {
                    assertCredentialPresence(legacyPreferences, expected = true)
                    assertCredentialPresence(credentialPreferences, expected = false)
                }

                2 -> {
                    assertCredentialPresence(legacyPreferences, expected = true)
                    assertMigratedCredentials(expectLegacyCleanup = false)
                }

                3 -> assertMigratedCredentials()
            }

            val expectedXAppToken = if (failedCommit > 1) {
                val retainedValue = "destination-value-after-phase-$failedCommit"
                assertTrue(
                    credentialPreferences.edit().putString("xAppToken", retainedValue).commit()
                )
                assertTrue(legacyPreferences.edit().putString("xAppToken", "").commit())
                retainedValue
            } else {
                STRING_CREDENTIAL_VALUES.getValue("xAppToken")
            }

            assertTrue(CredentialPreferencesMigration.migrate(legacyPreferences, credentialPreferences))
            assertMigratedCredentials(expectedXAppToken = expectedXAppToken)
            assertFalse(CredentialPreferencesMigration.migrate(legacyPreferences, credentialPreferences))
        }
    }

    @Test
    fun legacyFullBackupExcludesOnlyCredentialsPreferences() {
        val exclusions = sharedPreferenceExclusions(R.xml.backup_rules)

        assertEquals(setOf("full-backup-content"), exclusions.keys)
        assertEquals(setOf("credentials.xml"), exclusions["full-backup-content"])
    }

    @Test
    fun cloudBackupExcludesOnlyCredentialsPreferences() {
        val exclusions = sharedPreferenceExclusions(R.xml.data_extraction_rules)

        assertEquals(setOf("credentials.xml"), exclusions["cloud-backup"])
    }

    @Test
    fun deviceTransferExcludesOnlyCredentialsPreferences() {
        val exclusions = sharedPreferenceExclusions(R.xml.data_extraction_rules)

        assertEquals(setOf("credentials.xml"), exclusions["device-transfer"])
    }

    @Test
    fun ordinarySettingsRemainEligibleForEveryBackupMode() {
        val legacyPaths = sharedPreferenceExclusions(R.xml.backup_rules).values.flatten()
        val modernExclusions = sharedPreferenceExclusions(R.xml.data_extraction_rules)
        val modernPaths = modernExclusions.values.flatten()

        assertEquals(setOf("cloud-backup", "device-transfer"), modernExclusions.keys)
        (legacyPaths + modernPaths).forEach { excludedPath ->
            assertFalse("settings.xml must not be excluded", excludedPath == "settings.xml")
            assertFalse("All shared preferences must not be excluded", excludedPath == ".")
            assertFalse("All shared preferences must not be excluded", excludedPath == "/")
        }
    }

    private fun seedLegacyCredentials() {
        val editor = legacyPreferences.edit()
        BOOLEAN_CREDENTIAL_VALUES.forEach { (key, value) -> editor.putBoolean(key, value) }
        STRING_CREDENTIAL_VALUES.forEach { (key, value) -> editor.putString(key, value) }
        assertTrue(editor.commit())
    }

    private fun assertMigratedCredentials(
        expectLegacyCleanup: Boolean = true,
        expectedXAppToken: String = STRING_CREDENTIAL_VALUES.getValue("xAppToken"),
    ) {
        BOOLEAN_CREDENTIAL_VALUES.forEach { (key, value) ->
            assertEquals("Credential value mismatch for $key", value, credentialPreferences.getBoolean(key, !value))
            assertEquals("Legacy cleanup mismatch for $key", !expectLegacyCleanup, legacyPreferences.contains(key))
        }
        STRING_CREDENTIAL_VALUES.forEach { (key, value) ->
            val expectedValue = if (key == "xAppToken") expectedXAppToken else value
            assertEquals(
                "Credential value mismatch for $key",
                expectedValue,
                credentialPreferences.getString(key, null),
            )
            assertEquals("Legacy cleanup mismatch for $key", !expectLegacyCleanup, legacyPreferences.contains(key))
        }
        assertTrue("isLogin must be copied as true", credentialPreferences.getBoolean("isLogin", false))
        assertEquals(
            "The full credential key set must be represented in the destination",
            ALL_CREDENTIAL_KEYS,
            credentialPreferences.all.keys.intersect(ALL_CREDENTIAL_KEYS),
        )
    }

    private fun assertCredentialPresence(preferences: SharedPreferences, expected: Boolean) {
        ALL_CREDENTIAL_KEYS.forEach { key ->
            assertEquals("Unexpected presence for $key", expected, preferences.contains(key))
        }
    }

    private fun sharedPreferenceExclusions(resourceId: Int): Map<String, Set<String>> {
        val parser = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .resources
            .getXml(resourceId)
        val parentTags = ArrayDeque<String>()
        val exclusions = linkedMapOf<String, MutableSet<String>>()

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        if (tagName == "exclude" &&
                            parser.getAttributeValue(null, "domain") == "sharedpref"
                        ) {
                            val parentTag = parentTags.peekLast()
                                ?: throw AssertionError("sharedpref exclusion has no parent")
                            val path = parser.getAttributeValue(null, "path")
                                ?: throw AssertionError("sharedpref exclusion has no path")
                            exclusions.getOrPut(parentTag) { linkedSetOf() }.add(path)
                        }
                        parentTags.addLast(tagName)
                    }

                    XmlPullParser.END_TAG -> parentTags.removeLast()
                }
            }
        } finally {
            parser.close()
        }

        return exclusions
    }

    private fun clearPreferences() {
        check(legacyPreferences.edit().clear().commit())
        check(credentialPreferences.edit().clear().commit())
    }

    private companion object {
        val BOOLEAN_CREDENTIAL_VALUES = linkedMapOf(
            "isLogin" to true,
            "customToken" to true,
        )

        val STRING_CREDENTIAL_VALUES = listOf(
            "uid",
            "name",
            "token",
            "userAvatar",
            "level",
            "experience",
            "nextLevelExperience",
            "xAppToken",
            "xAppDevice",
            "MANUFACTURER",
            "BRAND",
            "MODEL",
            "BUILDNUMBER",
            "SDK_INT",
            "ANDROID_VERSION",
            "USER_AGENT",
            "SZLMID",
        ).associateWith { key -> "value-for-$key" }

        val ALL_CREDENTIAL_KEYS =
            (BOOLEAN_CREDENTIAL_VALUES.keys + STRING_CREDENTIAL_VALUES.keys).toSet()
    }
}

package com.godmiracle.coolapk.util

import android.content.SharedPreferences

internal object CredentialPreferencesMigration {

    private const val MIGRATION_MARKER = "_credentials_migration_v1"

    private val credentialKeys = setOf(
        "isLogin",
        "uid",
        "name",
        "token",
        "userAvatar",
        "level",
        "experience",
        "nextLevelExperience",
        "xAppToken",
        "xAppDevice",
        "customToken",
        "MANUFACTURER",
        "BRAND",
        "MODEL",
        "BUILDNUMBER",
        "SDK_INT",
        "ANDROID_VERSION",
        "USER_AGENT",
        "SZLMID",
    )

    fun migrate(
        legacyPreferences: SharedPreferences,
        credentialPreferences: SharedPreferences,
    ): Boolean = migrate(
        legacyPreferences = legacyPreferences,
        credentialPreferences = credentialPreferences,
        commitEditor = { editor -> editor.commit() },
    )

    internal fun migrate(
        legacyPreferences: SharedPreferences,
        credentialPreferences: SharedPreferences,
        commitEditor: (SharedPreferences.Editor) -> Boolean,
    ): Boolean {
        if (credentialPreferences.getBoolean(MIGRATION_MARKER, false)) {
            return false
        }

        val legacyValues = legacyPreferences.all
        val migratedKeys = mutableListOf<String>()
        val credentialEditor = credentialPreferences.edit()

        credentialKeys.forEach { key ->
            val value = legacyValues[key] ?: return@forEach
            if (!credentialPreferences.contains(key)) {
                when (value) {
                    is Boolean -> credentialEditor.putBoolean(key, value)
                    is String -> credentialEditor.putString(key, value)
                }
            }
            if (credentialPreferences.contains(key) || value is Boolean || value is String) {
                migratedKeys += key
            }
        }

        if (!commitEditor(credentialEditor)) {
            return false
        }

        val legacyEditor = legacyPreferences.edit()
        migratedKeys.forEach(legacyEditor::remove)
        if (!commitEditor(legacyEditor)) {
            return false
        }

        return commitEditor(
            credentialPreferences.edit()
                .putBoolean(MIGRATION_MARKER, true)
        )
    }
}

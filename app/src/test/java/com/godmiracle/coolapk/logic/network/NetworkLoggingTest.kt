package com.godmiracle.coolapk.logic.network

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLoggingTest {
    @Test
    fun `body logging is opt in and release cannot enable it`() {
        assertEquals(
            HttpLoggingInterceptor.Level.NONE,
            NetworkLogging.createInterceptor(debug = true, enableBodyLogging = false).level
        )
        assertEquals(
            HttpLoggingInterceptor.Level.BODY,
            NetworkLogging.createInterceptor(debug = true, enableBodyLogging = true).level
        )
        assertEquals(
            HttpLoggingInterceptor.Level.NONE,
            NetworkLogging.createInterceptor(debug = false, enableBodyLogging = true).level
        )
    }

    @Test
    fun `sensitive request data is redacted`() {
        val message = """
            Cookie: device=device-secret
            X-App-Token: app-token-secret
            {"secret":"secret-value","sts":"sts-secret","safe":"safe-value"}
            accessToken=query-secret&normal=value
        """.trimIndent()

        val redacted = NetworkLogging.redact(message)

        assertFalse(redacted.contains("device-secret"))
        assertFalse(redacted.contains("app-token-secret"))
        assertFalse(redacted.contains("secret-value"))
        assertFalse(redacted.contains("sts-secret"))
        assertFalse(redacted.contains("query-secret"))
        assertTrue(redacted.contains("[REDACTED]"))
        assertTrue(redacted.contains("safe-value"))
    }
}

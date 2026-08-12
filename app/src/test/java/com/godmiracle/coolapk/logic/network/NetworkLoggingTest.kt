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
            Cookie: uid=100; token=cookie-secret
            X-App-Token: app-token-secret
            {"password":"password-secret","captcha":"captcha-secret","sts":"sts-secret","safe":"safe-value"}
            accessToken=query-secret&normal=value
        """.trimIndent()

        val redacted = NetworkLogging.redact(message)

        assertFalse(redacted.contains("secret"))
        assertFalse(redacted.contains("query-secret"))
        assertTrue(redacted.contains("[REDACTED]"))
        assertTrue(redacted.contains("safe-value"))
    }
}

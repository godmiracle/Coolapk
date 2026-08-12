package com.godmiracle.coolapk.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenDeviceUtilsTest {
    @Test
    fun `token timestamp is an integer unix second`() {
        val timestamp = TokenDeviceUtils.currentUnixTimeSeconds()
        val now = System.currentTimeMillis() / 1000L

        assertTrue(timestamp.matches(Regex("\\d+")))
        assertTrue(kotlin.math.abs(timestamp.toLong() - now) <= 1L)
    }

    @Test
    fun `custom token is used only when enabled and non blank`() {
        assertEquals(
            "custom-token",
            TokenDeviceUtils.resolveAppToken("unused-device", true, " custom-token ")
        )
        assertTrue(
            TokenDeviceUtils.resolveAppToken("device", false, "custom-token").startsWith("v2")
        )
        assertTrue(
            TokenDeviceUtils.resolveAppToken("device", true, " ").startsWith("v2")
        )
    }
}

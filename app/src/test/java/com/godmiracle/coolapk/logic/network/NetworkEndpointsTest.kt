package com.godmiracle.coolapk.logic.network

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class NetworkEndpointsTest {
    @Test
    fun `all retrofit base urls end with a slash`() {
        NetworkEndpoints.all.forEach { baseUrl ->
            assertTrue(baseUrl, baseUrl.endsWith('/'))
            Retrofit.Builder().baseUrl(baseUrl).build()
        }
    }

    @Test
    fun `only trusted https hosts receive credential policy`() {
        assertTrue(NetworkEndpoints.isTrusted(Request.Builder().url("https://api.coolapk.com/v6/feed").build().url))
        assertTrue(NetworkEndpoints.isTrusted(Request.Builder().url("https://api2.coolapk.com/v8/feed").build().url))
        assertTrue(NetworkEndpoints.isTrusted(Request.Builder().url("https://account.coolapk.com/auth").build().url))
        assertFalse(NetworkEndpoints.isTrusted(Request.Builder().url("http://api.coolapk.com/v6/feed").build().url))
        assertFalse(NetworkEndpoints.isTrusted(Request.Builder().url("https://example.com/feed").build().url))
    }

    @Test
    fun `external request strips credential headers`() {
        val requestBuilder = Request.Builder().url("https://example.com/feed")
        NetworkCredentialPolicy.credentialHeaders.forEach { header ->
            requestBuilder.header(header, "secret")
        }
        requestBuilder.header("X-App-Future-Identity", "secret")

        val sanitized = NetworkCredentialPolicy.stripCredentials(requestBuilder.build())

        NetworkCredentialPolicy.credentialHeaders.forEach { header ->
            assertEquals(null, sanitized.header(header))
        }
        assertNull(sanitized.header("X-App-Future-Identity"))
    }
}

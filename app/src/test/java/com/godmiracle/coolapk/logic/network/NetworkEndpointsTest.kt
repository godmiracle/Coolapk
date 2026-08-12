package com.godmiracle.coolapk.logic.network

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
}

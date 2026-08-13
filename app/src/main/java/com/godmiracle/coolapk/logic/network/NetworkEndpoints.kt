package com.godmiracle.coolapk.logic.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal object NetworkEndpoints {
    const val API_BASE_URL = "https://api.coolapk.com/"
    const val API2_BASE_URL = "https://api2.coolapk.com/"
    const val ACCOUNT_BASE_URL = "https://account.coolapk.com/"

    val all = listOf(API_BASE_URL, API2_BASE_URL, ACCOUNT_BASE_URL)
    val trustedHosts = all.map { it.substringAfter("://").substringBefore('/') }.toSet()

    fun isTrusted(url: HttpUrl): Boolean {
        return url.isHttps && url.host in trustedHosts
    }
}

internal object NetworkCredentialPolicy {
    internal val apiIdentityHeaders = listOf(
        "Cookie",
        "User-Agent",
        "X-Requested-With",
        "X-Sdk-Int",
        "X-Sdk-Locale",
        "X-App-Id",
        "X-App-Token",
        "X-App-Version",
        "X-App-Code",
        "X-Api-Version",
        "X-App-Device",
        "X-Dark-Mode",
        "X-App-Channel",
        "X-App-Mode",
        "X-App-Supported"
    )

    internal val accountIdentityHeaders = listOf(
        "Cookie",
        "User-Agent",
        "X-Requested-With",
        "X-App-Id",
        "sec-ch-ua",
        "sec-ch-ua-mobile",
        "sec-ch-ua-platform",
        "Origin",
        "Referer",
        "Sec-Fetch-Site",
        "Sec-Fetch-Mode",
        "Sec-Fetch-User",
        "Sec-Fetch-Dest",
        "Upgrade-Insecure-Requests",
        "Accept-Language"
    )

    internal val credentialHeaders = (apiIdentityHeaders + accountIdentityHeaders).distinct()

    private val credentialHeaderNames = (
        credentialHeaders + listOf(
            "Authorization",
            "Proxy-Authorization",
            "Cookie2",
            "Token",
            "Upgrade=Insecure-Requests"
        )
    ).map { it.lowercase() }.toSet()

    fun sanitize(request: Request): Request {
        return if (NetworkEndpoints.isTrusted(request.url)) request else stripCredentials(request)
    }

    fun stripCredentials(request: Request): Request {
        val headersToRemove = request.headers.names().filter(::isCredentialHeader)
        if (headersToRemove.isEmpty()) return request

        return request.newBuilder().apply {
            headersToRemove.forEach(::removeHeader)
        }.build()
    }

    private fun isCredentialHeader(name: String): Boolean {
        val normalized = name.lowercase()
        return normalized in credentialHeaderNames ||
            normalized.startsWith("x-app-") ||
            normalized.startsWith("x-sdk-") ||
            normalized.startsWith("sec-ch-ua") ||
            normalized.startsWith("sec-fetch-")
    }
}

internal object NetworkCredentialBoundaryInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(NetworkCredentialPolicy.sanitize(chain.request()))
    }
}

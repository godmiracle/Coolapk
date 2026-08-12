package com.godmiracle.coolapk.logic.network

import android.util.Log
import com.godmiracle.coolapk.BuildConfig
import okhttp3.logging.HttpLoggingInterceptor

object NetworkLogging {
    private const val TAG = "CoolapkNetwork"
    private const val REDACTED = "[REDACTED]"

    private val sensitiveHeaderPattern = Regex(
        """(?im)^(\s*(?:authorization|proxy-authorization|cookie|set-cookie|x-app-token|x-app-device)\s*:\s*).*$"""
    )
    private val sensitiveValuePattern = Regex(
        """(?i)(["']?(?:authorization|proxy-authorization|cookie|set-cookie|x-app-token|x-app-device|token|access_token|accessToken|refresh_token|refreshToken|security_token|securityToken|customToken|password|passwd|pwd|captcha|verification_code|sms_code|sts|stsToken|access_key_secret|accessKeySecret|secret)\b["']?\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^,\s&;}]+)"""
    )

    fun createInterceptor(
        debug: Boolean = BuildConfig.DEBUG,
        enableBodyLogging: Boolean = BuildConfig.ENABLE_HTTP_BODY_LOGGING
    ): HttpLoggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, redact(message))
    }.apply {
        level = if (debug && enableBodyLogging) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    internal fun redact(message: String): String {
        val headersRedacted = sensitiveHeaderPattern.replace(message) {
            "${it.groupValues[1]}$REDACTED"
        }
        return sensitiveValuePattern.replace(headersRedacted) {
            "${it.groupValues[1]}$REDACTED"
        }
    }
}

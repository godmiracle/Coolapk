package com.godmiracle.coolapk.util

import com.godmiracle.coolapk.constant.Constants.APP_ID
import com.godmiracle.coolapk.constant.Constants.CHANNEL
import com.godmiracle.coolapk.constant.Constants.DARK_MODE
import com.godmiracle.coolapk.constant.Constants.LOCALE
import com.godmiracle.coolapk.constant.Constants.MODE
import com.godmiracle.coolapk.constant.Constants.REQUEST_WITH
import com.godmiracle.coolapk.util.CookieUtil.SESSID
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val deviceCode = TokenDeviceUtils.getLastingDeviceCode()
        val token = TokenDeviceUtils.resolveAppToken(
            deviceCode = deviceCode,
            customTokenEnabled = PrefManager.customToken,
            customToken = PrefManager.xAppToken,
        )
        builder.apply {
            addHeader("User-Agent", PrefManager.USER_AGENT)
            addHeader("X-Requested-With", REQUEST_WITH)
            addHeader("X-Sdk-Int", PrefManager.SDK_INT)
            addHeader("X-Sdk-Locale", LOCALE)
            addHeader("X-App-Id", APP_ID)
            addHeader("X-App-Token", token)
            addHeader("X-App-Version", PrefManager.VERSION_NAME)
            addHeader("X-App-Code", PrefManager.VERSION_CODE)
            addHeader("X-Api-Version", PrefManager.API_VERSION)
            addHeader("X-App-Device", deviceCode)
            addHeader("X-Dark-Mode", DARK_MODE)
            addHeader("X-App-Channel", CHANNEL)
            addHeader("X-App-Mode", MODE)
            addHeader("X-App-Supported", PrefManager.VERSION_CODE)
            addHeader("Content-Type", "application/x-www-form-urlencoded")
            if (PrefManager.isLogin)
                addHeader(
                    "Cookie",
                    "uid=${PrefManager.uid}; username=${PrefManager.username}; token=${PrefManager.token}"
                )
            else addHeader("Cookie", SESSID)
        }
        return chain.proceed(builder.build())
    }
}

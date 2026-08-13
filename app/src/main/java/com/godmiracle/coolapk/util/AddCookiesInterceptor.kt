package com.godmiracle.coolapk.util

import com.godmiracle.coolapk.constant.Constants.APP_ID
import com.godmiracle.coolapk.constant.Constants.CHANNEL
import com.godmiracle.coolapk.constant.Constants.DARK_MODE
import com.godmiracle.coolapk.constant.Constants.LOCALE
import com.godmiracle.coolapk.constant.Constants.MODE
import com.godmiracle.coolapk.constant.Constants.REQUEST_WITH
import com.godmiracle.coolapk.logic.network.NetworkCredentialPolicy
import com.godmiracle.coolapk.logic.network.NetworkEndpoints
import com.godmiracle.coolapk.util.CookieUtil.SESSID
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!NetworkEndpoints.isTrusted(request.url)) {
            return chain.proceed(NetworkCredentialPolicy.stripCredentials(request))
        }

        val builder: Request.Builder = request.newBuilder()
        val deviceCode = TokenDeviceUtils.getLastingDeviceCode()
        val token = TokenDeviceUtils.resolveAppToken(
            deviceCode = deviceCode,
            customTokenEnabled = PrefManager.customToken,
            customToken = PrefManager.xAppToken,
        )
        builder.apply {
            header("User-Agent", PrefManager.USER_AGENT)
            header("X-Requested-With", REQUEST_WITH)
            header("X-Sdk-Int", PrefManager.SDK_INT)
            header("X-Sdk-Locale", LOCALE)
            header("X-App-Id", APP_ID)
            header("X-App-Token", token)
            header("X-App-Version", PrefManager.VERSION_NAME)
            header("X-App-Code", PrefManager.VERSION_CODE)
            header("X-Api-Version", PrefManager.API_VERSION)
            header("X-App-Device", deviceCode)
            header("X-Dark-Mode", DARK_MODE)
            header("X-App-Channel", CHANNEL)
            header("X-App-Mode", MODE)
            header("X-App-Supported", PrefManager.VERSION_CODE)
            header("Content-Type", "application/x-www-form-urlencoded")
            if (PrefManager.isLogin)
                header(
                    "Cookie",
                    "uid=${PrefManager.uid}; username=${PrefManager.username}; token=${PrefManager.token}"
                )
            else header("Cookie", SESSID)
        }
        return chain.proceed(builder.build())
    }
}

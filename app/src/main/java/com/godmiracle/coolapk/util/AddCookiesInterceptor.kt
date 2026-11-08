package com.godmiracle.coolapk.util

import com.godmiracle.coolapk.logic.network.NetworkCredentialPolicy
import com.godmiracle.coolapk.logic.network.NetworkEndpoints
import com.godmiracle.coolapk.util.CookieUtil.SESSID
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

object AddCookiesInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!NetworkEndpoints.isTrusted(request.url)) {
            return chain.proceed(NetworkCredentialPolicy.stripCredentials(request))
        }

        val builder = request.newBuilder()
        listOf(
            "X-Dark-Mode",
            "X-App-Channel",
            "X-App-Mode",
            "X-App-Supported",
        ).forEach(builder::removeHeader)
        if (request.body == null) builder.removeHeader("Content-Type")

        builder.apply {
            IosDeviceUtils.headers().forEach { (name, value) -> header(name, value) }
            // The client is anonymous-only. Keep the server-issued anonymous session when
            // available, but never attach account cookies.
            header("Cookie", SESSID)
        }
        return chain.proceed(builder.build())
    }
}

package com.godmiracle.coolapk.logic.network

import com.godmiracle.coolapk.di.NetworkModule
import com.godmiracle.coolapk.util.AddCookiesInterceptor
import com.godmiracle.coolapk.util.CookieUtil
import com.godmiracle.coolapk.util.LoginCookiesInterceptor
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NetworkRequestBoundaryTest {
    private val serverCertificate = HeldCertificate.Builder()
        .commonName("request-boundary.test")
        .addSubjectAlternativeName(API_HOST)
        .addSubjectAlternativeName(API2_HOST)
        .addSubjectAlternativeName(ACCOUNT_HOST)
        .addSubjectAlternativeName(EXTERNAL_HOST)
        .build()
    private val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(serverCertificate)
        .build()
    private val clientCertificates = HandshakeCertificates.Builder()
        .addTrustedCertificate(serverCertificate.certificate)
        .build()
    private val loopbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return listOf(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
        }
    }
    private val identityHeaders = NetworkCredentialPolicy.credentialHeaders
        .associateWith { "sensitive-value" }
    private val passThroughInterceptor = Interceptor { chain ->
        chain.proceed(chain.request())
    }

    @Test
    fun `production clients install the final boundary without changing redirect semantics`() {
        val apiClient = NetworkModule.provideOkHttpClient()
        val noRedirectApiClient = NetworkModule.provideNoOkHttpClient()
        val accountClient = NetworkModule.provideAccountServiceOkHttpClient()

        assertTrue(apiClient.followRedirects)
        assertFalse(noRedirectApiClient.followRedirects)
        assertTrue(accountClient.followRedirects)
        assertSame(AddCookiesInterceptor, apiClient.networkInterceptors.first())
        assertSame(AddCookiesInterceptor, noRedirectApiClient.networkInterceptors.first())
        assertSame(LoginCookiesInterceptor, accountClient.interceptors.first())
        assertSame(NetworkCredentialBoundaryInterceptor, apiClient.networkInterceptors.last())
        assertSame(
            NetworkCredentialBoundaryInterceptor,
            noRedirectApiClient.networkInterceptors.last()
        )
        assertSame(NetworkCredentialBoundaryInterceptor, accountClient.networkInterceptors.last())
    }

    @Test
    fun `api client applies the boundary to every network request and redirect`() {
        val headerInjector = HeaderAddingInterceptor(identityHeaders)
        val client = configureTestTransport(
            NetworkModule.buildApiOkHttpClient(
                credentialInterceptor = headerInjector,
                loggingInterceptor = passThroughInterceptor,
                followRedirects = true
            )
        )

        assertClientBoundary(client, listOf(API_HOST, API2_HOST))
        assertEquals(6, headerInjector.invocationCount.get())
    }

    @Test
    fun `account client applies the boundary without rerunning login state injection`() {
        val headerInjector = HeaderAddingInterceptor(identityHeaders)
        val client = configureTestTransport(
            NetworkModule.buildAccountOkHttpClient(
                loginInterceptor = headerInjector,
                loggingInterceptor = passThroughInterceptor
            )
        )

        assertSame(headerInjector, client.interceptors.first())
        assertClientBoundary(client, listOf(ACCOUNT_HOST))
        assertEquals(4, headerInjector.invocationCount.get())
    }

    @Test
    fun `untrusted account request does not consume the login state flag`() {
        val client = configureTestTransport(
            NetworkModule.buildAccountOkHttpClient(
                loginInterceptor = LoginCookiesInterceptor,
                loggingInterceptor = passThroughInterceptor
            )
        )
        CookieUtil.isGetLoginParam = true

        try {
            withServer(https = false) { server ->
                server.enqueue(MockResponse().setResponseCode(200))

                execute(client, server.requestUrl(ACCOUNT_HOST, "/http-login"))

                assertTrue(CookieUtil.isGetLoginParam)
                val request = server.takeRecordedRequest()
                assertRecordedRequest(request, ACCOUNT_HOST, "http", server)
                assertIdentityHeadersAbsent(request)
            }
        } finally {
            CookieUtil.isGetLoginParam = false
        }
    }

    private fun assertClientBoundary(client: OkHttpClient, trustedHosts: List<String>) {
        trustedHosts.forEach { trustedHost ->
            withServer(https = true) { server ->
                server.enqueue(MockResponse().setResponseCode(200))

                execute(client, server.requestUrl(trustedHost, "/trusted"))

                val request = server.takeRecordedRequest()
                assertRecordedRequest(request, trustedHost, "https", server)
                assertIdentityHeadersPresent(request)
            }
        }

        withServer(https = true) { server ->
            server.enqueue(MockResponse().setResponseCode(200))

            execute(client, server.requestUrl(EXTERNAL_HOST, "/external"))

            val request = server.takeRecordedRequest()
            assertRecordedRequest(request, EXTERNAL_HOST, "https", server)
            assertIdentityHeadersAbsent(request)
        }

        withServer(https = false) { server ->
            server.enqueue(MockResponse().setResponseCode(200))

            execute(client, server.requestUrl(trustedHosts.first(), "/http"))

            val request = server.takeRecordedRequest()
            assertRecordedRequest(request, trustedHosts.first(), "http", server)
            assertIdentityHeadersAbsent(request)
        }

        withServer(https = true) { server ->
            val externalUrl = server.requestUrl(EXTERNAL_HOST, "/redirected")
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", externalUrl)
            )
            server.enqueue(MockResponse().setResponseCode(200))

            execute(client, server.requestUrl(trustedHosts.first(), "/redirect"))

            val initialRequest = server.takeRecordedRequest()
            val redirectedRequest = server.takeRecordedRequest()
            assertRecordedRequest(initialRequest, trustedHosts.first(), "https", server)
            assertRecordedRequest(redirectedRequest, EXTERNAL_HOST, "https", server)
            assertIdentityHeadersPresent(initialRequest)
            assertIdentityHeadersAbsent(redirectedRequest)
        }
    }

    private fun configureTestTransport(client: OkHttpClient): OkHttpClient {
        return client.newBuilder()
            .dns(loopbackDns)
            .sslSocketFactory(
                clientCertificates.sslSocketFactory(),
                clientCertificates.trustManager
            )
            .build()
    }

    private fun execute(client: OkHttpClient, url: HttpUrl) {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            assertEquals(response.message, 200, response.code)
        }
    }

    private fun assertIdentityHeadersPresent(request: RecordedRequest) {
        identityHeaders.forEach { (name, value) ->
            assertEquals(name, value, request.headers[name])
        }
    }

    private fun assertRecordedRequest(
        request: RecordedRequest,
        expectedHost: String,
        expectedScheme: String,
        server: MockWebServer
    ) {
        // MockWebServer reconstructs requestUrl from the server socket, so its host is localhost.
        // The logical authority sent by OkHttp is Host for HTTP/1.1 or :authority for HTTP/2.
        assertEquals(expectedScheme, request.requestUrl?.scheme)
        val authority = request.headers["Host"] ?: request.headers[":authority"]
        assertEquals("$expectedHost:${server.port}", authority)
    }

    private fun assertIdentityHeadersAbsent(request: RecordedRequest) {
        identityHeaders.keys.forEach { name ->
            assertNull(name, request.headers[name])
        }
    }

    private fun withServer(https: Boolean, block: (MockWebServer) -> Unit) {
        val server = MockWebServer()
        if (https) {
            server.useHttps(serverCertificates.sslSocketFactory(), false)
        }
        server.start()
        try {
            block(server)
        } finally {
            server.shutdown()
        }
    }

    private fun MockWebServer.requestUrl(host: String, path: String): HttpUrl {
        return url(path).newBuilder()
            .host(host)
            .build()
    }

    private fun MockWebServer.takeRecordedRequest(): RecordedRequest {
        return checkNotNull(takeRequest(5, TimeUnit.SECONDS)) {
            "MockWebServer did not receive a request"
        }
    }

    private class HeaderAddingInterceptor(
        private val headers: Map<String, String>
    ) : Interceptor {
        val invocationCount = AtomicInteger()

        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            invocationCount.incrementAndGet()
            val request = chain.request().newBuilder().apply {
                headers.forEach { (name, value) -> header(name, value) }
            }.build()
            return chain.proceed(request)
        }
    }

    private companion object {
        const val API_HOST = "api.coolapk.com"
        const val API2_HOST = "api2.coolapk.com"
        const val ACCOUNT_HOST = "account.coolapk.com"
        const val EXTERNAL_HOST = "external.example"
    }
}

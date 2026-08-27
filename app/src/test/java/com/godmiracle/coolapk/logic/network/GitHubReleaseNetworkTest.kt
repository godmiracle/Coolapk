package com.godmiracle.coolapk.logic.network

import com.godmiracle.coolapk.di.NetworkModule
import okhttp3.Dns
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.Protocol
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class GitHubReleaseNetworkTest {

    @Test
    fun `github client uses http 1 1 for api compatibility`() {
        assertEquals(
            listOf(Protocol.HTTP_1_1),
            NetworkModule.provideGitHubReleaseClient().protocols
        )
    }

    @Test
    fun `github client sends only github headers and fixed latest path`() {
        val certificate = HeldCertificate.Builder()
            .commonName("api.github.com")
            .addSubjectAlternativeName("api.github.com")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "tag_name":"v1.1.0",
                  "draft":false,
                  "prerelease":false
                }
                """.trimIndent()
            )
        )
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        try {
            val client = NetworkModule.provideGitHubReleaseClient().newBuilder()
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        return listOf(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
                    }
                })
                .sslSocketFactory(
                    clientCertificates.sslSocketFactory(),
                    clientCertificates.trustManager
                )
                .build()
            assertTrue(!client.followSslRedirects)
            val service = Retrofit.Builder()
                .baseUrl(server.url("/").newBuilder().host("api.github.com").build())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(GitHubReleaseApiService::class.java)

            val response = service.getLatestRelease().execute()
            val request = checkNotNull(server.takeRequest(5, TimeUnit.SECONDS))

            assertEquals(200, response.code())
            assertTrue(!client.followRedirects)
            assertEquals("/repos/godmiracle/Coolapk/releases/latest", request.path)
            assertEquals("application/vnd.github+json", request.getHeader("Accept"))
            assertEquals("2022-11-28", request.getHeader("X-GitHub-Api-Version"))
            assertEquals("Coolapk-Android", request.getHeader("User-Agent"))
            assertEquals("https", request.requestUrl?.scheme)
            assertNull(request.getHeader("Cookie"))
            assertNull(request.getHeader("X-App-Token"))
            assertNull(request.getHeader("X-App-Device"))
        } finally {
            server.shutdown()
        }
    }
}

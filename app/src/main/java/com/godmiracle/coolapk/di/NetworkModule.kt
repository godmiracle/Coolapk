package com.godmiracle.coolapk.di

import com.godmiracle.coolapk.logic.network.GitHubReleaseApiService
import com.godmiracle.coolapk.logic.network.ApiService
import com.godmiracle.coolapk.logic.network.NetworkCredentialBoundaryInterceptor
import com.godmiracle.coolapk.logic.network.NetworkEndpoints
import com.godmiracle.coolapk.logic.network.NetworkLogging
import com.godmiracle.coolapk.util.AddCookiesInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Api1Service

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Api1ServiceNoRedirect

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Api2Service

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubReleaseService

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GitHubReleaseClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Api1Service
    @Singleton
    @Provides
    fun provideApi1Service(@Api1Service retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Api1ServiceNoRedirect
    @Singleton
    @Provides
    fun provideApi1ServiceNo(@Api1ServiceNoRedirect retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Api2Service
    @Singleton
    @Provides
    fun provideApi2Service(@Api2Service retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @GitHubReleaseService
    @Singleton
    @Provides
    fun provideGitHubReleaseService(
        @GitHubReleaseService retrofit: Retrofit
    ): GitHubReleaseApiService {
        return retrofit.create(GitHubReleaseApiService::class.java)
    }

    @Api1Service
    @Singleton
    @Provides
    fun provideApi1ServiceRetrofit(@Api1Service okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkEndpoints.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Api1ServiceNoRedirect
    @Singleton
    @Provides
    fun provideApi1ServiceNoRetrofit(@Api1ServiceNoRedirect okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkEndpoints.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Api2Service
    @Singleton
    @Provides
    fun provideApi2ServiceRetrofit(@Api1Service okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkEndpoints.API2_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @GitHubReleaseService
    @Singleton
    @Provides
    fun provideGitHubReleaseRetrofit(
        @GitHubReleaseClient okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkEndpoints.GITHUB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Api1Service
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return buildApiOkHttpClient(
            credentialInterceptor = AddCookiesInterceptor,
            loggingInterceptor = NetworkLogging.createInterceptor(),
            followRedirects = true
        )
    }

    @Api1ServiceNoRedirect
    @Singleton
    @Provides
    fun provideNoOkHttpClient(): OkHttpClient {
        return buildApiOkHttpClient(
            credentialInterceptor = AddCookiesInterceptor,
            loggingInterceptor = NetworkLogging.createInterceptor(),
            followRedirects = false
        )
    }

    @GitHubReleaseClient
    @Singleton
    @Provides
    fun provideGitHubReleaseClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "Coolapk-Android")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val url = chain.request().url
                if (!url.isHttps || !isGitHubHost(url.host)) {
                    throw IOException("GitHub 请求重定向到不受支持的地址")
                }
                chain.proceed(chain.request())
            }
            .followRedirects(false)
            .followSslRedirects(false)
            .protocols(listOf(Protocol.HTTP_1_1))
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun isGitHubHost(host: String): Boolean {
        return host == "api.github.com"
    }

    internal fun buildApiOkHttpClient(
        credentialInterceptor: Interceptor,
        loggingInterceptor: Interceptor,
        followRedirects: Boolean
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor(credentialInterceptor)
            .addNetworkInterceptor(loggingInterceptor)
            .addNetworkInterceptor(NetworkCredentialBoundaryInterceptor)
            .followRedirects(followRedirects)
            .build()
    }

}

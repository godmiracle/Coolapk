package com.example.c001apk.di

import com.example.c001apk.logic.network.ApiService
import com.example.c001apk.logic.network.NetworkEndpoints
import com.example.c001apk.logic.network.NetworkLogging
import com.example.c001apk.util.AddCookiesInterceptor
import com.example.c001apk.util.LoginCookiesInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
annotation class AccountService

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

    @AccountService
    @Singleton
    @Provides
    fun provideAccountService(@AccountService retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
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

    @AccountService
    @Singleton
    @Provides
    fun provideAccountServiceRetrofit(@AccountService okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkEndpoints.ACCOUNT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    @Api1Service
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AddCookiesInterceptor)
            .addInterceptor(NetworkLogging.createInterceptor())
            .followRedirects(true)
            .build()
    }

    @Api1ServiceNoRedirect
    @Singleton
    @Provides
    fun provideNoOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AddCookiesInterceptor)
            .addInterceptor(NetworkLogging.createInterceptor())
            .followRedirects(false)
            .build()
    }

    @AccountService
    @Singleton
    @Provides
    fun provideAccountServiceOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(LoginCookiesInterceptor)
            .addInterceptor(NetworkLogging.createInterceptor())
            .build()
    }

}

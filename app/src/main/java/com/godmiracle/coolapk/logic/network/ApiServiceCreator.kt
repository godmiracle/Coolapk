package com.godmiracle.coolapk.logic.network

import com.godmiracle.coolapk.util.AddCookiesInterceptor
import com.godmiracle.coolapk.util.LoginCookiesInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class ServiceType {
    API_SERVICE,
    API2_SERVICE,
    ACCOUNT_SERVICE
}

object ApiServiceCreator {

    private fun getClient(serviceType: ServiceType, followRedirects: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                when (serviceType) {
                    ServiceType.API_SERVICE, ServiceType.API2_SERVICE -> AddCookiesInterceptor
                    ServiceType.ACCOUNT_SERVICE -> LoginCookiesInterceptor
                }
            )
            .addInterceptor(NetworkLogging.createInterceptor())
            .followRedirects(followRedirects)
            .build()


    private fun getRetrofit(serviceType: ServiceType, followRedirects: Boolean): Retrofit =
        Retrofit.Builder()
            .baseUrl(
                when (serviceType) {
                    ServiceType.API_SERVICE -> NetworkEndpoints.API_BASE_URL
                    ServiceType.API2_SERVICE -> NetworkEndpoints.API2_BASE_URL
                    ServiceType.ACCOUNT_SERVICE -> NetworkEndpoints.ACCOUNT_BASE_URL
                }
            )
            .addConverterFactory(GsonConverterFactory.create())
            .client(getClient(serviceType, followRedirects))
            .build()

    fun <T> create(serviceType: ServiceType, followRedirects: Boolean, serviceClass: Class<T>): T =
        getRetrofit(serviceType, followRedirects).create(serviceClass)

    inline fun <reified T> create(serviceType: ServiceType, followRedirects: Boolean = true): T =
        create(serviceType, followRedirects, T::class.java)

}

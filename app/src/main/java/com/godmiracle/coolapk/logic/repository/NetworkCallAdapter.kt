package com.godmiracle.coolapk.logic.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class EmptyResponseBodyException(statusCode: Int) :
    IOException("HTTP $statusCode returned an empty response body")

internal suspend fun <T> Call<T>.awaitResponse(
    requireBody: Boolean = true,
    allowRedirects: Boolean = false,
): Response<T> = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }

    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (!response.isSuccessful &&
                !(allowRedirects && response.code() in 300..399)
            ) {
                response.errorBody()?.close()
                continuation.resumeWithException(HttpException(response))
                return
            }

            if (requireBody && response.body() == null) {
                response.errorBody()?.close()
                continuation.resumeWithException(EmptyResponseBodyException(response.code()))
                return
            }

            continuation.resume(response)
        }

        override fun onFailure(call: Call<T>, throwable: Throwable) {
            if (!continuation.isActive) {
                return
            }

            when {
                throwable is CancellationException -> continuation.cancel(throwable)
                call.isCanceled -> continuation.cancel(
                    CancellationException("HTTP call was cancelled", throwable)
                )

                else -> continuation.resumeWithException(throwable)
            }
        }
    })
}

internal suspend fun <T> Call<T>.awaitRequiredBody(): T =
    awaitResponse(requireBody = true).body()!!

internal suspend fun <T> Call<T>.await(): T = awaitRequiredBody()

internal suspend fun <T> Call<T>.response(
    requireBody: Boolean = true,
    allowRedirects: Boolean = false,
): Response<T> = awaitResponse(requireBody, allowRedirects)

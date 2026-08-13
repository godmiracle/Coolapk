package com.godmiracle.coolapk.logic.repository

import com.godmiracle.coolapk.logic.model.HomeFeedResponse
import com.godmiracle.coolapk.logic.network.ApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NetworkCallAdapterTest {

    private val calls = mutableListOf<FakeCall<*>>()

    @Before
    fun setUp() {
        calls.clear()
    }

    @After
    fun tearDown() {
        calls.forEach(FakeCall<*>::close)
    }

    @Test
    fun `successful response returns decoded body`() = runBlocking {
        val call = fakeCall(Response.success(TestPayload("ok")))

        assertEquals("ok", call.awaitRequiredBody().value)
    }

    @Test
    fun `empty successful body is a failure`() = runBlocking {
        val call = fakeCall(Response.success<TestPayload>(null))

        val error = assertThrows(EmptyResponseBodyException::class.java) {
            runBlocking { call.awaitRequiredBody() }
        }

        assertTrue(error.message.orEmpty().contains("HTTP 200"))
    }

    @Test
    fun `http error statuses are failures with status diagnostics`() = runBlocking {
        listOf(401, 403, 404, 500).forEach { statusCode ->
            val call = fakeCall(
                Response.error<TestPayload>(
                    statusCode,
                    ResponseBody.create(null, "secret-error")
                )
            )

            val error = assertThrows(HttpException::class.java) {
                runBlocking { call.awaitRequiredBody() }
            }

            assertEquals(statusCode, error.code())
        }
    }

    @Test
    fun `transport failure is propagated`() = runBlocking {
        val call = fakeCall<TestPayload>(failure = IOException("offline"))

        val error = assertThrows(IOException::class.java) {
            runBlocking { call.awaitRequiredBody() }
        }

        assertEquals("offline", error.message)
    }

    @Test
    fun `cancelling coroutine cancels underlying call`() = runBlocking {
        val call = fakeCall<TestPayload>(holdResponse = true, callbackOnCancel = true)
        val request = launch(start = CoroutineStart.UNDISPATCHED) {
            call.awaitRequiredBody()
        }

        assertTrue(call.enqueued.await(2, TimeUnit.SECONDS))
        request.cancelAndJoin()

        assertTrue(call.cancelled.await(2, TimeUnit.SECONDS))
        assertTrue(call.cancellationFailureDelivered.await(2, TimeUnit.SECONDS))
        assertTrue(call.isCanceled)
    }

    @Test
    fun `CancellationException from onFailure remains cancellation`() = runBlocking {
        val call = fakeCall<TestPayload>(
            failure = CancellationException("cancelled by transport"),
        )

        val error = try {
            call.awaitRequiredBody()
            throw AssertionError("Expected CancellationException")
        } catch (error: CancellationException) {
            error
        }

        assertEquals("cancelled by transport", error.message)
    }

    @Test
    fun `onFailure after direct call cancellation remains cancellation`() = runBlocking {
        val call = fakeCall<TestPayload>(holdResponse = true, callbackOnCancel = true)
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            call.awaitRequiredBody()
        }

        assertTrue(call.enqueued.await(2, TimeUnit.SECONDS))
        call.cancel()

        val error = try {
            request.await()
            throw AssertionError("Expected CancellationException")
        } catch (error: CancellationException) {
            error
        }
        assertTrue(error.message.orEmpty().contains("cancelled"))
        assertTrue(call.cancellationFailureDelivered.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun `NetworkRepo cancellation terminates flow without an emission`() = runBlocking {
        val call = fakeCall<HomeFeedResponse>(holdResponse = true, callbackOnCancel = true)
        val repository = networkRepo(call)
        var emissionCount = 0
        val collection = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.getHomeFeed(1, 0, "install-time", null, null).collect {
                emissionCount += 1
            }
        }

        assertTrue(call.enqueued.await(2, TimeUnit.SECONDS))
        collection.cancelAndJoin()

        assertTrue(call.cancelled.await(2, TimeUnit.SECONDS))
        assertTrue(call.cancellationFailureDelivered.await(2, TimeUnit.SECONDS))
        assertEquals(0, emissionCount)
    }

    @Test
    fun `NetworkRepo emits transport failure with original cause`() = runBlocking {
        val transportFailure = IOException("offline")
        val call = fakeCall<HomeFeedResponse>(failure = transportFailure)
        val repository = networkRepo(call)

        val result = repository.getHomeFeed(1, 0, "install-time", null, null).single()

        assertTrue(result.isFailure)
        assertSame(transportFailure, result.exceptionOrNull())
    }

    @Test
    fun `redirect response can be inspected without accepting other errors`() = runBlocking {
        val rawResponse = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.coolapk.com/download").build())
            .protocol(Protocol.HTTP_1_1)
            .code(302)
            .message("Found")
            .header("Location", "https://download.example/app.apk")
            .build()
        val call = fakeCall(
            Response.error<TestPayload>(ResponseBody.create(null, "redirect"), rawResponse)
        )

        val response = call.awaitResponse(requireBody = false, allowRedirects = true)

        assertEquals(302, response.code())
        assertEquals("https://download.example/app.apk", response.headers()["Location"])
    }

    private fun <T> fakeCall(
        response: Response<T>? = null,
        failure: Throwable? = null,
        holdResponse: Boolean = false,
        callbackOnCancel: Boolean = false,
    ): FakeCall<T> = FakeCall(response, failure, holdResponse, callbackOnCancel).also(calls::add)

    @Suppress("UNCHECKED_CAST")
    private fun networkRepo(homeFeedCall: Call<HomeFeedResponse>): NetworkRepo {
        val service = Proxy.newProxyInstance(
            ApiService::class.java.classLoader,
            arrayOf(ApiService::class.java),
        ) { _, method, _ ->
            if (method.name == "getHomeFeed") {
                homeFeedCall
            } else {
                throw UnsupportedOperationException("Unexpected ApiService call: ${method.name}")
            }
        } as ApiService
        return NetworkRepo(service, service, service, service)
    }

    private data class TestPayload(val value: String)

    private class FakeCall<T>(
        private val response: Response<T>? = null,
        private val failure: Throwable? = null,
        private val holdResponse: Boolean = false,
        private val callbackOnCancel: Boolean = false,
    ) : Call<T> {
        val enqueued = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val cancellationFailureDelivered = CountDownLatch(1)
        private val request = Request.Builder().url("https://api.coolapk.com/test").build()
        private val cancellationCallbackDelivered = AtomicBoolean(false)
        @Volatile private var executed = false
        @Volatile private var canceled = false
        @Volatile private var heldCallback: Callback<T>? = null

        override fun request() = request

        override fun timeout(): Timeout = Timeout.NONE

        override fun isExecuted() = executed

        override fun isCanceled() = canceled

        override fun clone(): Call<T> =
            FakeCall(response, failure, holdResponse, callbackOnCancel)

        override fun execute(): Response<T> {
            executed = true
            return response ?: throw (failure ?: IOException("no response"))
        }

        override fun enqueue(callback: Callback<T>) {
            executed = true
            if (holdResponse) {
                heldCallback = callback
            }
            enqueued.countDown()
            if (holdResponse) {
                notifyCancellationCallbackIfNeeded()
                return
            }
            Thread {
                if (canceled) {
                    callback.onFailure(this, IOException("cancelled"))
                } else if (failure != null) {
                    callback.onFailure(this, failure)
                } else {
                    callback.onResponse(this, response!!)
                }
            }.start()
        }

        override fun cancel() {
            canceled = true
            cancelled.countDown()
            notifyCancellationCallbackIfNeeded()
        }

        fun close() {
            cancel()
        }

        private fun notifyCancellationCallbackIfNeeded() {
            if (!canceled || !callbackOnCancel) {
                return
            }
            val callback = heldCallback ?: return
            if (cancellationCallbackDelivered.compareAndSet(false, true)) {
                callback.onFailure(this, IOException("cancelled"))
                cancellationFailureDelivered.countDown()
            }
        }
    }
}

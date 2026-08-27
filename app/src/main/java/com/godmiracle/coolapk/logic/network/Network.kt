package com.godmiracle.coolapk.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Network {

    private val apiService = ApiServiceCreator.create<ApiService>(ServiceType.API_SERVICE)
    private val apiServiceNoRedirect =
        ApiServiceCreator.create<ApiService>(ServiceType.API_SERVICE, false)
    private val api2Service = ApiServiceCreator.create<ApiService>(ServiceType.API2_SERVICE)

    suspend fun getHomeFeed(
        page: Int,
        firstLaunch: Int,
        installTime: String,
        firstItem: String?,
        lastItem: String?
    ) = api2Service.getHomeFeed(page, firstLaunch, installTime, firstItem, lastItem).await()

    suspend fun getFeedContent(id: String, rid: String?) =
        api2Service.getFeedContent(id, rid).await()

    suspend fun getFeedContentReply(
        id: String,
        listType: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
        discussMode: Int,
        feedType: String,
        blockStatus: Int,
        fromFeedAuthor: Int
    ) =
        api2Service.getFeedContentReply(
            id,
            listType,
            page,
            firstItem,
            lastItem,
            discussMode,
            feedType,
            blockStatus,
            fromFeedAuthor
        ).await()

    suspend fun getSearch(
        type: String,
        feedType: String,
        sort: String,
        keyWord: String,
        pageType: String?,
        pageParam: String?,
        page: Int,
        lastItem: String?,
    ) = apiService.getSearch(
        type,
        feedType,
        sort,
        keyWord,
        pageType,
        pageParam,
        page,
        lastItem,
    ).await()

    suspend fun getReply2Reply(id: String, page: Int, lastItem: String?) =
        apiService.getReply2Reply(id, page, lastItem).await()

    suspend fun getTopicLayout(tag: String) = api2Service.getTopicLayout(tag).await()

    suspend fun getProductLayout(id: String) = apiService.getProductLayout(id).await()

    suspend fun getUserSpace(uid: String) = apiService.getUserSpace(uid).await()

    suspend fun getUserFeed(uid: String, page: Int, lastItem: String?) =
        apiService.getUserFeed(uid, page, lastItem).await()

    suspend fun getAppInfo(id: String) = apiService.getAppInfo(id).await()

    suspend fun getAppDownloadLink(pn: String, aid: String, vc: String) =
        apiServiceNoRedirect.getAppDownloadLink(pn, aid, vc).response()

    suspend fun getProfile(uid: String) = api2Service.getProfile(uid).await()

    suspend fun getFollowList(url: String, uid: String, page: Int, lastItem: String?) =
        apiService.getFollowList(url, uid, page, lastItem).await()

    suspend fun getDataList(
        url: String,
        title: String,
        subTitle: String?,
        lastItem: String?,
        page: Int
    ) = apiService.getDataList(url, title, subTitle, lastItem, page).await()

    suspend fun getDyhDetail(dyhId: String, type: String, page: Int, lastItem: String?) =
        apiService.getDyhDetail(dyhId, type, page, lastItem).await()

    suspend fun getVoteComment(
        fid: String,
        extraKey: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
    ) = apiService.getVoteComment(fid, extraKey, page, firstItem, lastItem).await()

    suspend fun getProductList() = apiService.getProductList().await()

    suspend fun getCollectionList(
        url: String,
        uid: String?,
        id: String?,
        showDefault: Int,
        page: Int,
        lastItem: String?
    ) = apiService.getCollectionList(url, uid, id, showDefault, page, lastItem).await()

    private suspend fun <T> Call<T>.await(): T {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if (body != null) continuation.resume(body)
                    else continuation.resumeWithException(
                        RuntimeException("response body is null")
                    )
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    private suspend fun <T> Call<T>.response(): Response<T> {
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    continuation.resume(response)
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

}

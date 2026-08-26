package com.godmiracle.coolapk.logic.network

import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import kotlin.coroutines.CoroutineContext

object Repository {

    suspend fun getHomeFeed(
        page: Int,
        firstLaunch: Int,
        installTime: String,
        firstItem: String?,
        lastItem: String?
    ) = fire {
        Result.success(Network.getHomeFeed(page, firstLaunch, installTime, firstItem, lastItem))
    }

    suspend fun getFeedContent(id: String, rid: String?) = fire {
        Result.success(Network.getFeedContent(id, rid))
    }

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
    ) = fire {
        Result.success(
            Network.getFeedContentReply(
                id,
                listType,
                page,
                firstItem,
                lastItem,
                discussMode,
                feedType,
                blockStatus,
                fromFeedAuthor
            )
        )
    }

    suspend fun getSearch(
        type: String, feedType: String, sort: String, keyWord: String, pageType: String?,
        pageParam: String?, page: Int, lastItem: String?
    ) = fire {
        Result.success(
            Network.getSearch(
                type, feedType, sort, keyWord, pageType, pageParam, page, lastItem
            )
        )
    }

    suspend fun getReply2Reply(id: String, page: Int, lastItem: String?) = fire {
        Result.success(Network.getReply2Reply(id, page, lastItem))
    }

    suspend fun getTopicLayout(tag: String) = fire {
        Result.success(Network.getTopicLayout(tag))
    }

    suspend fun getProductLayout(id: String) = fire {
        Result.success(Network.getProductLayout(id))
    }

    suspend fun getUserSpace(uid: String) = fire {
        Result.success(Network.getUserSpace(uid))
    }

    suspend fun getUserFeed(uid: String, page: Int, lastItem: String?) = fire {
        Result.success(Network.getUserFeed(uid, page, lastItem))
    }

    suspend fun getAppInfo(id: String) = fire {
        Result.success(Network.getAppInfo(id))
    }

    suspend fun getAppDownloadLink(pn: String, aid: String, vc: String) = fire {
        val appResponse = Network.getAppDownloadLink(pn, aid, vc)
        Result.success(appResponse.headers()["Location"])
    }

    suspend fun getAppsUpdate(pkgs: String) = fire {
        val multipartBody =
            MultipartBody.Part.createFormData("pkgs", pkgs)
        val appResponse = Network.getAppsUpdate(multipartBody)
        Result.success(appResponse.data)
    }

    suspend fun getProfile(uid: String) = fire {
        Result.success(Network.getProfile(uid))
    }

    suspend fun getFollowList(url: String, uid: String, page: Int, lastItem: String?) = fire {
        Result.success(Network.getFollowList(url, uid, page, lastItem))
    }

    suspend fun getDataList(
        url: String, title: String, subTitle: String?, lastItem: String?, page: Int
    ) = fire {
        Result.success(Network.getDataList(url, title, subTitle, lastItem, page))
    }

    suspend fun getDyhDetail(dyhId: String, type: String, page: Int, lastItem: String?) = fire {
        Result.success(Network.getDyhDetail(dyhId, type, page, lastItem))
    }

    suspend fun getVoteComment(
        fid: String, extraKey: String, page: Int, firstItem: String?, lastItem: String?,
    ) = fire(Dispatchers.IO) {
        Result.success(Network.getVoteComment(fid, extraKey, page, firstItem, lastItem))
    }

    suspend fun getProductList() = fire {
        Result.success(Network.getProductList())
    }

    suspend fun getCollectionList(
        url: String,
        uid: String?,
        id: String?,
        showDefault: Int,
        page: Int,
        lastItem: String?
    ) =
        fire {
            Result.success(Network.getCollectionList(url, uid, id, showDefault, page, lastItem))
        }

    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }
            emit(result)
        }

    private fun <T> fire(block: suspend () -> Result<T>) =
        flow {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure(e)
            }
            emit(result)
        }.flowOn(Dispatchers.IO)

}

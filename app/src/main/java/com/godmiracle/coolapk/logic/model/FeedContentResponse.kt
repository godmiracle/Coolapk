package com.godmiracle.coolapk.logic.model

data class FeedContentResponse(
    val status: Int?,
    val error: Int?,
    val message: String?,
    val messageStatus: Int?,
    val data: HomeFeedResponse.Data?
)


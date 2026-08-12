package com.godmiracle.coolapk.logic.model

data class FeedArticleContentBean(
    var data: List<Data>?,
) {
    data class Data(
        val type: String?,
        val message: String?,
        val url: String?,
        val description: String?,
        val title: String?,
        val subTitle: String?,
        val logo: String?,
    )
}
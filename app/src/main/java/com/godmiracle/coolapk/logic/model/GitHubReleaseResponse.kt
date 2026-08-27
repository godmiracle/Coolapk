package com.godmiracle.coolapk.logic.model

import com.google.gson.annotations.SerializedName

data class GitHubReleaseResponse(
    @SerializedName("tag_name") val tagName: String?,
    val name: String?,
    val body: String?,
    @SerializedName("html_url") val htmlUrl: String?,
    @SerializedName("published_at") val publishedAt: String?,
    val draft: Boolean?,
    val prerelease: Boolean?
)

package com.godmiracle.coolapk.logic.network

import com.godmiracle.coolapk.logic.model.GitHubReleaseResponse
import retrofit2.Call
import retrofit2.http.GET

interface GitHubReleaseApiService {

    @GET("repos/godmiracle/Coolapk/releases/latest")
    fun getLatestRelease(): Call<GitHubReleaseResponse>
}

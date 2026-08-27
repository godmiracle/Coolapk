package com.godmiracle.coolapk.logic.repository

import com.godmiracle.coolapk.di.GitHubReleaseService
import com.godmiracle.coolapk.logic.network.GitHubReleaseApiService
import com.godmiracle.coolapk.logic.update.GitHubReleaseValidator
import com.godmiracle.coolapk.logic.update.ValidatedGitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubReleaseRepository @Inject constructor(
    @GitHubReleaseService private val apiService: GitHubReleaseApiService
) {

    suspend fun getLatestRelease(): ValidatedGitHubRelease = withContext(Dispatchers.IO) {
        GitHubReleaseValidator.validate(apiService.getLatestRelease().await())
    }
}

package com.godmiracle.coolapk.logic.update

import com.godmiracle.coolapk.logic.model.GitHubReleaseResponse

class GitHubReleaseValidationException(message: String) : IllegalArgumentException(message)

data class ValidatedGitHubRelease(
    val tagName: String,
    val version: GitHubReleaseVersion,
    val title: String,
    val body: String,
    val htmlUrl: String?,
    val publishedAt: String?
)

object GitHubReleaseValidator {
    fun validate(release: GitHubReleaseResponse): ValidatedGitHubRelease {
        if (release.draft != false) {
            throw GitHubReleaseValidationException("GitHub Release 仍是草稿")
        }
        if (release.prerelease != false) {
            throw GitHubReleaseValidationException("GitHub Release 是预发布版本")
        }

        val tagName = release.tagName?.trim().orEmpty()
        val version = GitHubReleaseVersion.parse(tagName)
            ?: throw GitHubReleaseValidationException("Release 版本号格式无效")

        return ValidatedGitHubRelease(
            tagName = tagName,
            version = version,
            title = release.name?.trim().takeUnless { it.isNullOrEmpty() } ?: tagName,
            body = release.body?.trim().takeUnless { it.isNullOrEmpty() } ?: "未提供更新说明",
            htmlUrl = release.htmlUrl,
            publishedAt = release.publishedAt
        )
    }
}

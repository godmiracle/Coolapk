package com.godmiracle.coolapk.logic.update

private val versionPattern = Regex(
    "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:[-+][0-9A-Za-z.-]+)?$"
)

data class GitHubReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<GitHubReleaseVersion> {

    override fun compareTo(other: GitHubReleaseVersion): Int {
        return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String?): GitHubReleaseVersion? {
            val match = value?.trim()?.let(versionPattern::matchEntire) ?: return null
            val numbers = match.groupValues.drop(1).take(3).map(String::toIntOrNull)
            if (numbers.any { it == null }) return null
            return GitHubReleaseVersion(
                major = numbers[0]!!,
                minor = numbers[1]!!,
                patch = numbers[2]!!
            )
        }
    }
}

enum class GitHubReleaseUpdateStatus {
    AVAILABLE,
    UP_TO_DATE,
    CURRENT_NEWER,
    CURRENT_DEVELOPMENT
}

fun compareGitHubReleaseVersion(
    currentVersionName: String?,
    latestVersion: GitHubReleaseVersion
): GitHubReleaseUpdateStatus {
    val currentVersion = GitHubReleaseVersion.parse(currentVersionName)
        ?: return GitHubReleaseUpdateStatus.CURRENT_DEVELOPMENT
    return when {
        latestVersion > currentVersion -> GitHubReleaseUpdateStatus.AVAILABLE
        latestVersion == currentVersion -> GitHubReleaseUpdateStatus.UP_TO_DATE
        else -> GitHubReleaseUpdateStatus.CURRENT_NEWER
    }
}

package com.godmiracle.coolapk.logic.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseVersionTest {

    @Test
    fun `parses release versions and ignores v prefix`() {
        assertEquals(GitHubReleaseVersion(1, 2, 3), GitHubReleaseVersion.parse("v1.2.3"))
        assertEquals(GitHubReleaseVersion(10, 0, 7), GitHubReleaseVersion.parse("10.0.7"))
        assertEquals(GitHubReleaseVersion(1, 2, 3), GitHubReleaseVersion.parse(" v1.2.3 "))
    }

    @Test
    fun `rejects non semver names`() {
        assertNull(GitHubReleaseVersion.parse("def065d"))
        assertNull(GitHubReleaseVersion.parse("v1.2"))
        assertNull(GitHubReleaseVersion.parse("v01.2.3"))
        assertNull(GitHubReleaseVersion.parse("v999999999999999999.2.3"))
    }

    @Test
    fun `compares a formal release with the current build`() {
        assertEquals(
            GitHubReleaseUpdateStatus.AVAILABLE,
            compareGitHubReleaseVersion("v1.0.0", GitHubReleaseVersion(1, 1, 0))
        )
        assertEquals(
            GitHubReleaseUpdateStatus.UP_TO_DATE,
            compareGitHubReleaseVersion("1.1.0", GitHubReleaseVersion(1, 1, 0))
        )
        assertEquals(
            GitHubReleaseUpdateStatus.CURRENT_NEWER,
            compareGitHubReleaseVersion("v1.2.0", GitHubReleaseVersion(1, 1, 0))
        )
        assertEquals(
            GitHubReleaseUpdateStatus.CURRENT_DEVELOPMENT,
            compareGitHubReleaseVersion("def065d", GitHubReleaseVersion(1, 1, 0))
        )
        assertTrue(GitHubReleaseVersion(1, 10, 0) > GitHubReleaseVersion(1, 2, 0))
    }
}

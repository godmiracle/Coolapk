package com.godmiracle.coolapk.logic.update

import com.godmiracle.coolapk.logic.model.GitHubReleaseResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GitHubReleaseValidatorTest {

    @Test
    fun `validates stable release metadata`() {
        val validated = GitHubReleaseValidator.validate(release())

        assertEquals("v1.1.0", validated.tagName)
        assertEquals(GitHubReleaseVersion(1, 1, 0), validated.version)
        assertEquals("Release 1.1.0", validated.title)
        assertEquals("Notes", validated.body)
    }

    @Test
    fun `rejects draft and prerelease`() {
        assertThrows(GitHubReleaseValidationException::class.java) {
            GitHubReleaseValidator.validate(release(draft = true))
        }
        assertThrows(GitHubReleaseValidationException::class.java) {
            GitHubReleaseValidator.validate(release(prerelease = true))
        }
    }

    @Test
    fun `rejects invalid release version`() {
        assertThrows(GitHubReleaseValidationException::class.java) {
            GitHubReleaseValidator.validate(release(tagName = "latest"))
        }
    }

    private fun release(
        tagName: String = "v1.1.0",
        draft: Boolean? = false,
        prerelease: Boolean? = false
    ) = GitHubReleaseResponse(
        tagName = tagName,
        name = "Release 1.1.0",
        body = "Notes",
        htmlUrl = "https://github.com/godmiracle/Coolapk/releases/tag/v1.1.0",
        publishedAt = "2026-08-26T00:00:00Z",
        draft = draft,
        prerelease = prerelease
    )
}

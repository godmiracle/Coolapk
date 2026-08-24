package com.godmiracle.coolapk.ui.topic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicSortUrlTest {

    private val discussionUrl =
        "/page?url=/product/feedList?type=feed&id=42&ignoreEntityById=1"
    private val encodedDiscussionUrl =
        "/page?url=%2Fproduct%2FfeedList%3FcacheExpires%3D60%26type%3Dfeed%26id%3D42%26listType%3Dlastupdate_desc"

    @Test
    fun `default sort keeps the original tab url`() {
        assertEquals(
            discussionUrl,
            TopicSortUrl.forSort(discussionUrl, TopicSort.DEFAULT)
        )
    }

    @Test
    fun `latest sort appends dateline order to a nested url`() {
        val sorted = TopicSortUrl.forSort(discussionUrl, TopicSort.LATEST)

        assertTrue(sorted.endsWith("&listType=dateline_desc"))
        assertTrue(sorted.contains("id=42"))
    }

    @Test
    fun `hot sort replaces an existing list type`() {
        val url = "$discussionUrl&listType=dateline_desc"
        val sorted = TopicSortUrl.forSort(url, TopicSort.HOT)

        assertTrue(sorted.endsWith("&listType=rank_score"))
        assertFalse(sorted.contains("listType=dateline_desc"))
    }

    @Test
    fun `sort works when the base url has no query`() {
        assertEquals(
            "/page?listType=rank_score",
            TopicSortUrl.forSort("/page", TopicSort.HOT)
        )
    }

    @Test
    fun `hot sort replaces list type inside an encoded nested url`() {
        val sorted = TopicSortUrl.forSort(encodedDiscussionUrl, TopicSort.HOT)

        assertTrue(sorted.contains("%26listType%3Drank_score"))
        assertFalse(sorted.contains("%26listType%3Dlastupdate_desc"))
        assertFalse(sorted.contains("&listType=rank_score"))
    }

    @Test
    fun `latest sort replaces list type inside an encoded nested url`() {
        val sorted = TopicSortUrl.forSort(encodedDiscussionUrl, TopicSort.LATEST)

        assertTrue(sorted.contains("%26listType%3Ddateline_desc"))
        assertFalse(sorted.contains("%26listType%3Dlastupdate_desc"))
        assertFalse(sorted.contains("&listType=dateline_desc"))
    }
}

package com.godmiracle.coolapk.ui.topic

enum class TopicSort(
    val label: String,
    val listType: String?
) {
    DEFAULT("默认", null),
    LATEST("最新", "dateline_desc"),
    HOT("热度", "rank_score");

    companion object {
        fun fromLabel(label: String): TopicSort? = when (label) {
            "默认", "最近回复" -> DEFAULT
            "最新", "最新发布" -> LATEST
            "热度", "热度排序" -> HOT
            else -> null
        }
    }
}

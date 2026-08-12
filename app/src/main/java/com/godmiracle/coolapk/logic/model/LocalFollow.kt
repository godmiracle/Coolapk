package com.godmiracle.coolapk.logic.model

import androidx.room.Entity

@Entity(
    tableName = "local_follow",
    primaryKeys = ["type", "targetId"]
)
data class LocalFollow(
    val type: String,
    val targetId: String,
    val title: String,
    val avatar: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

object LocalFollowType {
    const val TOPIC = "topic"
    const val PRODUCT = "product"
}

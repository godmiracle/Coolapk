package com.godmiracle.coolapk.logic.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TopicBean(
    val url: String,
    val title: String
): Parcelable
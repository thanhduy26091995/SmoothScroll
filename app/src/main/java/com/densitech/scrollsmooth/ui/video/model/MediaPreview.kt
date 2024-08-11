package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaPreview(
    val url: String,
    val startTime: Int,
    val endTime: Int
)

package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaMetadata(
    val duration: Int,
    val width: Double,
    val height: Double,
    val bitrate: String,
    val codec: String
)

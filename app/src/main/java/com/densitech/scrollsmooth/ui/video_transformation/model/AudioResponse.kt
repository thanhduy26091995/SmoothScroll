package com.densitech.scrollsmooth.ui.video_transformation.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioResponse(
    @SerialName("audio_url")
    val audioUrl: String,
    val name: String,
    val artist: String,
    val duration: Int,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String,
    val source: String
)

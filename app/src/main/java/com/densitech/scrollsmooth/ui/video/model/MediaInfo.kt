package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaInfo(
    val status: String,
    @SerialName("video_id")
    val videoId: String,
    @SerialName("video_url")
    val videoUrl: String,
    @SerialName("metadata")
    val metadata: MediaMetadata,
    val thumbnails: MediaThumbnail,
    val previews: List<MediaPreview>,
    val title: String,
    val tags: List<String>,
    val owner: MediaOwner,
)
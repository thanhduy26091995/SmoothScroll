package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaThumbnailDetail(
    val thumbnailUrl: String,
    val time: Int
)

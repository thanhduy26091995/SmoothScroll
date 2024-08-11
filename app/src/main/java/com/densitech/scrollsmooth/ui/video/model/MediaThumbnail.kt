package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaThumbnail(
    val small: List<MediaThumbnailDetail>,
    val medium: List<MediaThumbnailDetail>
)

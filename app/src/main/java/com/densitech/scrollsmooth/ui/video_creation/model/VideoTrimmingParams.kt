package com.densitech.scrollsmooth.ui.video_creation.model

data class VideoTrimmingParams(
    val videoDuration: Long,
    val startPosition: Long,
    val currentPosition: Long,
    val endPosition: Long,
    val numberOfThumbnailFrame: Int
)

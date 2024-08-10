package com.densitech.scrollsmooth.ui.video.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaInfo(
    val url: String,
    val bitrate: Double,
    val width: Double,
    val height: Double
)

data class PagingResponse(
    val currentPage: Int,
    val pageSize: Int,
    val totalPage: Int,
    val items: List<MediaInfo>,
    val nextPage: Int?
)
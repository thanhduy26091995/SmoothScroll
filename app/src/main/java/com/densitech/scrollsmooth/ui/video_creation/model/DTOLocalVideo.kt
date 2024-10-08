package com.densitech.scrollsmooth.ui.video_creation.model

import kotlinx.serialization.Serializable

@Serializable
data class DTOLocalVideo(
    val id: Long,
    val videoName: String,
    val duration: String,
    val videoPath: String,
    val width: Int,
    val height: Int
)

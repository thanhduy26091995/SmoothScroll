package com.densitech.scrollsmooth.ui.video.model

import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool

data class VideoItemParams(
    val playerPool: PlayerPool,
    val isActive: Boolean,
    val currentToken: Int,
    val currentMediaSource: MediaSource,
    val mediaInfo: MediaInfo,
    val isDownloaded: Boolean,
)

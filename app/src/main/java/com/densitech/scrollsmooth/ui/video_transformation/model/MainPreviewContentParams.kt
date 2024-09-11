package com.densitech.scrollsmooth.ui.video_transformation.model

import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams

data class MainPreviewContentParams(
    val exoPlayer: ExoPlayer,
    val currentFraction: Float,
    val isShowingTextOverlay: Boolean,
    val textOverlayList: List<TextOverlayParams>,
)

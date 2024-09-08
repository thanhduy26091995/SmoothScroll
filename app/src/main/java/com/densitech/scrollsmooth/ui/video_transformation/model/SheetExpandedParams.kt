package com.densitech.scrollsmooth.ui.video_transformation.model

import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalThumbnail
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo

data class SheetExpandedParams(
    val thumbnails: List<DTOLocalThumbnail>,
    val selectedVideo: DTOLocalVideo,
    val isVideoPlaying: Boolean,
    val startPosition: Long,
    val currentPlayingPosition: Long,
    val endPosition: Long
)

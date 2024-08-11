package com.densitech.scrollsmooth.ui.video.prefetch

import androidx.media3.common.MediaItem

class MediaItemSource(val mediaItems: List<MediaItem>) {

    fun get(index: Int): MediaItem {
        val uri = mediaItems[index.mod(mediaItems.size)]
        return uri
    }
}
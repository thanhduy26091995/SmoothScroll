package com.densitech.scrollsmooth.ui.video.preFetch

import androidx.media3.common.MediaItem

class MediaItemSource(val mediaItems: List<MediaItem>) {
    var lCacheSize: Int = 2
    var rCacheSize: Int = 7

    private val slidingWindowCache = HashMap<Int, MediaItem>()

    private fun getRaw(index: Int): MediaItem {
        return mediaItems[index.mod(mediaItems.size)]
    }

    private fun getCached(index: Int): MediaItem {
        var mediaItem = slidingWindowCache[index]
        if (mediaItem == null) {
            mediaItem = getRaw(index)
            slidingWindowCache[index] = mediaItem

            slidingWindowCache.remove(index - lCacheSize - 1)
            slidingWindowCache.remove(index + rCacheSize + 1)
        }

        return mediaItem
    }

    fun get(index: Int): MediaItem {
        return getCached(index)
    }

    fun get(fromIndex: Int, toIndex: Int): List<MediaItem> {
        val result: MutableList<MediaItem> = mutableListOf()
        for (i in fromIndex..toIndex) {
            result.add(get(i))
        }
        return result
    }
}
package com.densitech.scrollsmooth.ui.video.preFetch

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

class MediaItemSource(val mediaItems: List<MediaItem>) {
    var lCacheSize: Int = 2
    var rCacheSize: Int = 7

    private val slidingWindowCache = HashMap<Int, MediaItem>()

    private fun getRaw(index: Int): MediaItem {
        return mediaItems[index.mod(mediaItems.size)]
    }

    @OptIn(UnstableApi::class)
    private fun getCached(index: Int): MediaItem {
        var mediaItem = slidingWindowCache[index]
        if (mediaItem == null) {
            mediaItem = getRaw(index)
            slidingWindowCache[index] = mediaItem
            Log.d("viewpager", "Put URL ${mediaItem.localConfiguration?.uri} into sliding cache")
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
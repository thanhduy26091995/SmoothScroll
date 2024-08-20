package com.densitech.scrollsmooth.ui.video_creation

import android.graphics.Bitmap
import androidx.collection.LruCache

class VideoThumbnailCache(maxSize: Int) : LruCache<String, Bitmap>(maxSize) {
    override fun sizeOf(key: String, value: Bitmap): Int {
        return value.byteCount / 1024 // Returns size in kilobytes
    }
}
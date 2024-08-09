package com.densitech.scrollsmooth.ui.video.preFetch

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object CacheSingleton {
    @Volatile
    private var instance: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        return instance ?: synchronized(this) {
            instance ?: buildCache(context).also { instance = it }
        }
    }

    private fun buildCache(context: Context): SimpleCache {
        val databaseProvider = StandaloneDatabaseProvider(context)
        val downloadDir = context.getExternalFilesDir(null) ?: context.filesDir
        val cacheDir = File(downloadDir, "downloads")
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            if (!created) {
                throw IllegalStateException("Failed to create cache directory: ${cacheDir.absolutePath}")
            }
        }
        val maxBytes = 100 * 1024 * 1024L // 300MB
        return SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(maxBytes), databaseProvider)
    }

    fun release() {
        synchronized(this) {
            instance?.release()
            instance = null
        }
    }
}

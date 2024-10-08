package com.densitech.scrollsmooth.ui.video.prefetch

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel.Companion.CACHED_DOWNLOAD_FOLDER
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
        val cacheDir = File(downloadDir, CACHED_DOWNLOAD_FOLDER)
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            if (!created) {
                throw IllegalStateException("Failed to create cache directory: ${cacheDir.absolutePath}")
            }
        }
        val maxBytes = 200 * 1024 * 1024L // 200MB
        return SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(maxBytes), databaseProvider)
    }

    fun release() {
        synchronized(this) {
            instance?.release()
            instance = null
        }
    }
}

package com.densitech.scrollsmooth.ui.downloader

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import com.densitech.scrollsmooth.ui.downloader.SmoothScrollDownloadService.Companion.DOWNLOAD_FOLDER
import org.chromium.net.CronetEngine
import java.io.File
import java.util.concurrent.Executors

@UnstableApi
object DownloadManagerSingleton {
    @Volatile
    private var instance: DownloadManager? = null

    fun getInstance(context: Context): DownloadManager {
        return instance ?: synchronized(this) {
            instance ?: buildDownloadManager(context)
                .also { instance = it }
        }
    }

    private fun buildDownloadManager(context: Context): DownloadManager {
        val cronetEngine = CronetEngine.Builder(context).build()
        val cronetDataSourceFactory = CronetDataSource.Factory(
            cronetEngine,
            Executors.newSingleThreadExecutor()
        )

        return DownloadManager(
            context,
            StandaloneDatabaseProvider(context),
            buildCache(context),
            cronetDataSourceFactory,
            Executors.newSingleThreadExecutor()
        )
    }

    private fun buildCache(context: Context): SimpleCache {
        val databaseProvider = StandaloneDatabaseProvider(context)
        val downloadDir = context.getExternalFilesDir(null) ?: context.filesDir
        val cacheDir = File(downloadDir, DOWNLOAD_FOLDER)
        if (!cacheDir.exists()) {
            val created = cacheDir.mkdirs()
            if (!created) {
                throw IllegalStateException("Failed to create cache directory: ${cacheDir.absolutePath}")
            }
        }
        return SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
    }
}
package com.densitech.scrollsmooth.ui.downloader

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import com.densitech.scrollsmooth.ui.downloader.SmoothScrollDownloadService.Companion.DOWNLOAD_FOLDER
import com.densitech.scrollsmooth.ui.video.prefetch.CacheSingleton
import org.chromium.net.CronetEngine
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
            CacheSingleton.getInstance(context, DOWNLOAD_FOLDER),
            cronetDataSourceFactory,
            Executors.newSingleThreadExecutor()
        )
    }
}
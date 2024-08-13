package com.densitech.scrollsmooth.ui.downloader

import android.app.Notification
import android.content.Context
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.video.prefetch.CacheSingleton
import org.chromium.net.CronetEngine
import java.util.concurrent.Executors

@UnstableApi
class SmoothScrollDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.app_name,
    0
) {
    companion object {
        private const val JOB_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL = 1000L
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val DOWNLOAD_FOLDER = "video_downloaded"
    }

    override fun getDownloadManager(): DownloadManager {
        val downloadManager = getDownloadManager(this)
        val downloadNotificationHelper =
            DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        downloadManager.addListener(
            TerminalStateNotificationHelper(
                this,
                downloadNotificationHelper,
                FOREGROUND_NOTIFICATION_ID + 1
            )
        )
        return downloadManager
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadNotificationHelper(
            this,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID
        ).buildProgressNotification(
            this,
            R.drawable.ic_download,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    private fun getDownloadManager(context: Context): DownloadManager {
        // Cronet
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

    class TerminalStateNotificationHelper(
        private val context: Context,
        private val notificationHelper: DownloadNotificationHelper,
        firstNotificationId: Int
    ) : DownloadManager.Listener {

        private var nextNotificationId = 0

        init {
            nextNotificationId = firstNotificationId
        }

        private lateinit var notification: Notification

        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?
        ) {
            if (download.state == Download.STATE_COMPLETED) {
                notification = notificationHelper.buildDownloadCompletedNotification(
                    context,
                    R.drawable.ic_download_done,  /* contentIntent= */
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
                return
            }

            if (download.state == Download.STATE_FAILED) {
                notification = notificationHelper.buildDownloadFailedNotification(
                    context,
                    R.drawable.ic_download_done,
                    null,
                    Util.fromUtf8Bytes(download.request.data)
                )
                NotificationUtil.setNotification(context, nextNotificationId++, notification)
                return
            }
        }
    }
}
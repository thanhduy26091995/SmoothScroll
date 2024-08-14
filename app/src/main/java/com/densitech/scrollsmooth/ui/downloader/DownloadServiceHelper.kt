package com.densitech.scrollsmooth.ui.downloader

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.offline.*
import com.densitech.scrollsmooth.ui.video.model.MediaInfo
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

@UnstableApi
class DownloadServiceHelper @OptIn(UnstableApi::class) constructor(
    val context: Context,
    private val dataSourceFactory: DataSource.Factory,
    downloadManager: DownloadManager
) : DownloadHelper.Callback {

    interface Listener {
        fun onDownloadCompleted(videoId: String)

        fun onDownloadRemoved(videoId: String)
    }

    private val listeners: CopyOnWriteArraySet<Listener> = CopyOnWriteArraySet()
    private val downloads: HashMap<Uri, Download> = HashMap()
    private var downloadIndex: DownloadIndex = downloadManager.downloadIndex
    private var mediaItemNeedDownload: MediaItem? = null
    private val downloadedVideoList: ArrayList<MediaInfo> = arrayListOf()

    init {
        downloadManager.addListener(DownloadManagerState())
        loadDownloads()
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    // For this version, only support download for single file only
    fun downloadClick(mediaItem: MediaItem) {
        val renderersFactory = DefaultRenderersFactory(context)
        val download = downloads[mediaItem.localConfiguration?.uri]
        if (download != null && download.state != Download.STATE_FAILED) {
            DownloadService.sendRemoveDownload(
                context,
                SmoothScrollDownloadService::class.java,
                download.request.id,
                false
            )
            return
        }

        val downloadHelper =
            DownloadHelper.forMediaItem(context, mediaItem, renderersFactory, dataSourceFactory)
        mediaItemNeedDownload = mediaItem
        downloadHelper.prepare(this)

        Toast.makeText(context, "Downloading video", Toast.LENGTH_LONG).show()
    }

    override fun onPrepared(helper: DownloadHelper) {
        // Download prepared successfully, starting to get a format
        val format = getFirstFormatWithDrmInitData(helper)
        // Single download
        if (format == null) {
            if (helper.periodCount == 0) {
                startDownload(helper)
                helper.release()
                return
            }
            return
        }
    }

    fun isDownloaded(mediaItem: MediaItem): Boolean {
        val uri = mediaItem.localConfiguration?.uri ?: return false
        val download = downloads[uri]
        return download != null && download.state != Download.STATE_FAILED
    }

    fun getDownloadedVideo(): List<MediaInfo> {
        return downloadedVideoList
    }

    override fun onPrepareError(helper: DownloadHelper, e: IOException) {
        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
    }

    private fun loadDownloads() {
        try {
            val loadedDownloads = downloadIndex.getDownloads()
            while (loadedDownloads.moveToNext()) {
                val download = loadedDownloads.download
                val downloadDataStr = Util.fromUtf8Bytes(download.request.data)
                val mediaInfo = Json.decodeFromString<MediaInfo>(downloadDataStr)
                downloadedVideoList.add(mediaInfo)
                downloads[download.request.uri] = download
            }
        } catch (e: Exception) {
            print("Failed to query downloads: ${e.message}")
        }
    }

    private fun buildDownloadRequest(helper: DownloadHelper): DownloadRequest {
        mediaItemNeedDownload?.mediaMetadata?.extras?.let { bundle ->
            val mediaInfoStr = bundle.getString(VideoScreenViewModel.EXTRAS_METADATA) ?: return@let
            return helper.getDownloadRequest(
                Util.getUtf8Bytes(mediaInfoStr)
            )
        }
        throw Exception("No MediaItem to download")
    }

    private fun startDownload(helper: DownloadHelper) {
        DownloadService.sendAddDownload(
            context,
            SmoothScrollDownloadService::class.java,
            buildDownloadRequest(helper),  /* foreground= */
            false
        )
    }

    private fun getFirstFormatWithDrmInitData(helper: DownloadHelper): Format? {
        for (i in 0..<helper.periodCount) {
            val mappedTrackInfo = helper.getMappedTrackInfo(i)
            for (renderIndex in 0..<mappedTrackInfo.rendererCount) {
                val trackGroups = mappedTrackInfo.getTrackGroups(renderIndex)
                for (trackGroupIndex in 0..<trackGroups.length) {
                    val trackGroup = trackGroups[trackGroupIndex]
                    for (formatIndex in 0..<trackGroup.length) {
                        val format = trackGroup.getFormat(formatIndex)
                        if (format.drmInitData != null) {
                            return format
                        }
                    }
                }
            }
        }
        return null
    }

    inner class DownloadManagerState : DownloadManager.Listener {
        override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: java.lang.Exception?
        ) {
            super.onDownloadChanged(downloadManager, download, finalException)
            if (download.state == Download.STATE_COMPLETED) {
                downloads[download.request.uri] = download
                for (listener in listeners) {
                    listener.onDownloadCompleted(download.request.id)
                }
            }
        }

        override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
            super.onDownloadRemoved(downloadManager, download)
            downloads.remove(download.request.uri)
            for (listener in listeners) {
                listener.onDownloadRemoved(download.request.id)
            }
        }
    }
}
package com.densitech.scrollsmooth.ui.utils

import androidx.media3.common.C
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

@UnstableApi
class CustomAnalyticsListener : AnalyticsListener {
    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        super.onLoadStarted(eventTime, loadEventInfo, mediaLoadData)
        logLoadEvent("Load started", loadEventInfo, mediaLoadData)
    }

    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        super.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData)
        logLoadEvent("Load completed", loadEventInfo, mediaLoadData)
    }

    override fun onLoadCanceled(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        super.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData)
        logLoadEvent("Load canceled", loadEventInfo, mediaLoadData)
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        super.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled)
        logLoadEvent("Load error", loadEventInfo, mediaLoadData)
    }

    private fun logLoadEvent(
        eventName: String,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        val dataSourceType = when (mediaLoadData.dataType) {
            C.DATA_TYPE_MEDIA -> "media"
            C.DATA_TYPE_MANIFEST -> "manifest"
            C.DATA_TYPE_TIME_SYNCHRONIZATION -> "time synchronization"
            else -> "other"
        }

        val fromCache = loadEventInfo.dataSpec.uri.scheme == "file" // Simple heuristic
        Log.d(
            "CustomAnalyticsListener",
            "$eventName: type=$dataSourceType, fromCache=$fromCache, uri=${loadEventInfo.dataSpec.uri}"
        )
    }
}

package com.densitech.scrollsmooth.ui.video.preFetch

import android.annotation.SuppressLint
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.PreloadMediaSource
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.Allocator
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.video.VideoRendererEventListener

@SuppressLint("UnsafeOptInUsageError")
class MediaSourceManager(
    private val mediaSourceFactory: MediaSource.Factory,
    preloadLooper: Looper,
    allocator: Allocator,
    rendersFactory: RenderersFactory,
    trackSelector: TrackSelector,
    bandwidthMeter: BandwidthMeter
) {
    private val mediaSourcesThread = HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)
    private var handler: Handler
    private var sourceMap: MutableMap<MediaItem, PreloadMediaSource> = HashMap()
    private var preloadMediaSourceFactory: PreloadMediaSource.Factory

    init {
        mediaSourcesThread.start()
        handler = Handler(mediaSourcesThread.looper)
        trackSelector.init({}, bandwidthMeter)
        preloadMediaSourceFactory = PreloadMediaSource.Factory(
            mediaSourceFactory,
            PreloadControlImpl(targetPreloadPositionUs = 5_000_000L),
            trackSelector,
            bandwidthMeter,
            getRendererCapabilities(rendersFactory),
            allocator,
            preloadLooper
        )
    }

    fun add(mediaItem: MediaItem) {
        if (!sourceMap.containsKey(mediaItem)) {
            val preloadMediaSource = preloadMediaSourceFactory.createMediaSource(mediaItem)
            sourceMap[mediaItem] = preloadMediaSource
            handler.post { preloadMediaSource.preload(/* startPositionUs= */ 0L) }
        }
    }

    fun addAlls(mediaItems: List<MediaItem>) {
        mediaItems.forEach {
            add(it)
        }
    }

    operator fun get(mediaItem: MediaItem): PreloadMediaSource {
        if (!sourceMap.containsKey(mediaItem)) {
            add(mediaItem)
        }

        return sourceMap[mediaItem]!!
    }

    fun release() {
        sourceMap.keys.forEach {
            sourceMap[it]?.releasePreloadMediaSource()
            handler.removeCallbacksAndMessages(null)
            mediaSourcesThread.quit()
        }
    }

    @UnstableApi
    private fun getRendererCapabilities(
        renderersFactory: RenderersFactory
    ): Array<RendererCapabilities> {
        val renderers =
            renderersFactory.createRenderers(
                Util.createHandlerForCurrentOrMainLooper(),
                object : VideoRendererEventListener {},
                object : AudioRendererEventListener {},
                { _: CueGroup? -> }
            ) { _: Metadata ->
            }
        val capabilities = ArrayList<RendererCapabilities>()
        for (i in renderers.indices) {
            capabilities.add(renderers[i].capabilities)
        }
        return capabilities.toTypedArray()
    }

    private class PreloadControlImpl(private val targetPreloadPositionUs: Long) :
        PreloadMediaSource.PreloadControl {
        override fun onSourcePrepared(mediaSource: PreloadMediaSource): Boolean {
            return true
        }

        override fun onTracksSelected(mediaSource: PreloadMediaSource): Boolean {
            return true
        }

        override fun onContinueLoadingRequested(
            mediaSource: PreloadMediaSource,
            bufferedPositionUs: Long
        ): Boolean {
            return bufferedPositionUs < targetPreloadPositionUs
        }

        override fun onUsedByPlayer(mediaSource: PreloadMediaSource) {

        }
    }
}
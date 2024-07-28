package com.densitech.scrollsmooth.ui.video

import android.content.Context
import android.os.HandlerThread
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.preload.PreloadMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.densitech.scrollsmooth.ui.video.preFetch.MediaItemSource
import com.densitech.scrollsmooth.ui.video.preFetch.MediaSourceManager
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoScreenViewModel @Inject constructor() : ViewModel() {
    private val _playList: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())

    private val _playerPool: MutableStateFlow<PlayerPool?> = MutableStateFlow(null)
    val playerPool = _playerPool.asStateFlow()

    private val _mediaItemSource: MutableStateFlow<MediaItemSource?> = MutableStateFlow(null)
    val mediaItemSource = _mediaItemSource.asStateFlow()

    private lateinit var mediaSourceManager: MediaSourceManager

    private val playbackThread: HandlerThread =
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)

    private val holderMap: MutableMap<Int, ExoPlayer> = mutableMapOf()

    fun initData(context: Context) {
        _playList.value = listOf(
            MediaItem.fromUri("https://bestvpn.org/html5demos/assets/dizzy.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-avc-baseline-480.mp4"),
            MediaItem.fromUri("https://html5demos.com/assets/dizzy.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_1.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_2.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_3.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_4.mp4"),
        )

        playbackThread.start()
        val renderersFactory = DefaultRenderersFactory(context)
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs= */ 15_000,
                    /* maxBufferMs= */ 15_000,
                    /* bufferForPlaybackMs= */ 500,
                    /* bufferForPlaybackAfterRebufferMs= */ 1_000
                )
                .build()

        _playerPool.value = PlayerPool(
            3,
            context,
            playbackThread.looper,
            loadControl,
            renderersFactory,
            DefaultBandwidthMeter.getSingletonInstance(context)
        )

        mediaSourceManager = MediaSourceManager(
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context)),
            playbackThread.looper,
            loadControl.allocator,
            renderersFactory,
            DefaultTrackSelector(context),
            DefaultBandwidthMeter.getSingletonInstance(context)
        )

        _mediaItemSource.value = MediaItemSource(_playList.value)
    }

    fun addNewMediaItems(mediaItems: List<MediaItem>) {
        if (this::mediaSourceManager.isInitialized) {
            mediaSourceManager.addAlls(mediaItems)
        }
    }

    fun getMediaSourceByMediaItem(mediaItem: MediaItem): PreloadMediaSource? {
        return if (this::mediaSourceManager.isInitialized) {
            mediaSourceManager[mediaItem]
        } else {
            null
        }
    }

    fun onPlayerReady(token: Int, exoPlayer: ExoPlayer?) {
        exoPlayer?.let {
            holderMap[token] = exoPlayer
        }
    }

    fun onPlayerDestroy(token: Int) {
        holderMap.remove(token)
    }

    fun play(position: Int) {
        holderMap[position]?.let {
            _playerPool.value?.play(it)
        }
    }
}

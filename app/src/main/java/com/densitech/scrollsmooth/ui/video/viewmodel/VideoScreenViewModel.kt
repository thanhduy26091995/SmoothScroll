package com.densitech.scrollsmooth.ui.video.viewmodel

import android.content.Context
import android.os.HandlerThread
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.Status.STAGE_LOADED_TO_POSITION_MS
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.densitech.scrollsmooth.ui.video.preFetch.MediaItemSource
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.abs

@UnstableApi
@HiltViewModel
class VideoScreenViewModel @Inject constructor() : ViewModel() {
    private val _playList: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())

    private val _playerPool: MutableStateFlow<PlayerPool?> = MutableStateFlow(null)
    val playerPool = _playerPool.asStateFlow()

    private val _mediaItemSource: MutableStateFlow<MediaItemSource?> = MutableStateFlow(null)
    val mediaItemSource = _mediaItemSource.asStateFlow()

    private lateinit var preloadManager: DefaultPreloadManager
    private val currentMediaItemsAndIndexes: ArrayDeque<Pair<MediaItem, Int>> = ArrayDeque()
    val holderMap: MutableMap<Int, ExoPlayer> = mutableMapOf()
    private val holderRatioMap: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
    var currentPlayingIndex: Int = C.INDEX_UNSET
        private set

    companion object {
        private const val TAG = "VideoScreenViewModel"
        private const val LOAD_CONTROL_MIN_BUFFER_MS = 5_000
        private const val LOAD_CONTROL_MAX_BUFFER_MS = 20_000
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS = 500
        private const val MANAGED_ITEM_COUNT = 5
        private const val ITEM_ADD_REMOVE_COUNT = 5
        private const val NUMBER_OF_PLAYERS = 5
    }

    private val playbackThread: HandlerThread =
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)

    fun initData(context: Context) {
        _playList.value = listOf(
            MediaItem.Builder().setUri("https://bestvpn.org/html5demos/assets/dizzy.mp4")
                .setMediaId("0").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-avc-baseline-480.mp4")
                .setMediaId("1").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_1.mp4")
                .setMediaId("2").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_2.mp4")
                .setMediaId("3").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_3.mp4")
                .setMediaId("3").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_4.mp4")
                .setMediaId("4").build(),
            MediaItem.Builder()
                .setUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_6.mp4")
                .setMediaId("5").build()
        )
        _mediaItemSource.value = MediaItemSource(_playList.value)

        playbackThread.start()
        val renderersFactory = DefaultRenderersFactory(context)
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    LOAD_CONTROL_MIN_BUFFER_MS,
                    LOAD_CONTROL_MAX_BUFFER_MS,
                    LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

        _playerPool.value = PlayerPool(
            NUMBER_OF_PLAYERS,
            context,
            playbackThread.looper,
            loadControl,
            renderersFactory,
            DefaultBandwidthMeter.getSingletonInstance(context)
        )

        val trackSelector = DefaultTrackSelector(context)
        trackSelector.init({}, DefaultBandwidthMeter.getSingletonInstance(context))

        preloadManager = DefaultPreloadManager(
            DefaultPreloadControl(),
            DefaultMediaSourceFactory(context),
            trackSelector,
            DefaultBandwidthMeter.getSingletonInstance(context),
            DefaultRendererCapabilitiesList.Factory(renderersFactory),
            loadControl.allocator,
            playbackThread.looper
        )

        for (i in 0 until ITEM_ADD_REMOVE_COUNT) {
            addMediaItem(index = i, isAddingToRight = true)
        }

        preloadManager.invalidate()
    }

    fun getMediaSourceByMediaItem(mediaItem: MediaItem, index: Int): MediaSource? {
        var mediaSource: MediaSource? = null
        if (this::preloadManager.isInitialized) {
            mediaSource = preloadManager.getMediaSource(mediaItem)
            if (mediaSource == null) {
                preloadManager.add(mediaItem, index)
                mediaSource = preloadManager.getMediaSource(mediaItem)
            }
        }
        return mediaSource
    }

    fun onReceiveRatio(token: Int, width: Int, height: Int) {
        holderRatioMap[token] = Pair(width, height)
    }

    fun getCurrentRatio(token: Int): Pair<Int, Int> {
        val currentValue = holderRatioMap[token] ?: return Pair(0, 0)
        return currentValue
    }

    fun onPlayerReady(token: Int, exoPlayer: ExoPlayer?) {
        exoPlayer?.let {
            holderMap[token] = exoPlayer
        }

        if (!currentMediaItemsAndIndexes.isEmpty()) {
            val leftMostIndex = currentMediaItemsAndIndexes.first().second
            val rightMostIndex = currentMediaItemsAndIndexes.last().second

            if (rightMostIndex - token <= ITEM_ADD_REMOVE_COUNT) {
                Log.d(TAG, "onViewAttachedToWindow: Approaching to the rightmost item")
                for (i in 1 until ITEM_ADD_REMOVE_COUNT + 1) {
                    addMediaItem(index = rightMostIndex + i, isAddingToRight = true)
                    removeMediaItem(isRemovingFromRightEnd = false)
                }
            } else if (token - leftMostIndex <= 2) {
                Log.d(TAG, "onViewAttachedToWindow: Approaching to the leftmost item")
                for (i in 1 until ITEM_ADD_REMOVE_COUNT + ITEM_ADD_REMOVE_COUNT) {
                    addMediaItem(index = leftMostIndex - i, isAddingToRight = false)
                    removeMediaItem(isRemovingFromRightEnd = true)
                }
            }
        }
    }

    fun onPlayerDestroy(token: Int) {
        holderMap.remove(token)
    }

    fun play(position: Int) {
        currentPlayingIndex = position
        holderMap[position]?.let {
            _playerPool.value?.play(it)
        }
        preloadManager.setCurrentPlayingIndex(currentPlayingIndex)
        preloadManager.invalidate()
    }

    private fun addMediaItem(index: Int, isAddingToRight: Boolean) {
        if (index < 0) {
            return
        }

        val mediaItem = _mediaItemSource.value?.get(index) ?: return
        preloadManager.add(mediaItem, index)
        if (isAddingToRight) {
            currentMediaItemsAndIndexes.addLast(Pair(mediaItem, index))
        } else {
            currentMediaItemsAndIndexes.addFirst(Pair(mediaItem, index))
        }
    }

    private fun removeMediaItem(isRemovingFromRightEnd: Boolean) {
        if (currentMediaItemsAndIndexes.size <= MANAGED_ITEM_COUNT) {
            return
        }

        val itemAndIndex = if (isRemovingFromRightEnd) {
            currentMediaItemsAndIndexes.removeLast()
        } else {
            currentMediaItemsAndIndexes.removeFirst()
        }

        Log.d(TAG, "removeMediaItem: Removing item at index ${itemAndIndex.second}")
        preloadManager.remove(itemAndIndex.first)
    }

    inner class DefaultPreloadControl : TargetPreloadStatusControl<Int> {
        override fun getTargetPreloadStatus(rankingData: Int): TargetPreloadStatusControl.PreloadStatus? {
            if (abs(rankingData - currentPlayingIndex) == 2) {
                return DefaultPreloadManager.Status(STAGE_LOADED_TO_POSITION_MS, 500L)
            } else if (abs(rankingData - currentPlayingIndex) == 1) {
                return DefaultPreloadManager.Status(STAGE_LOADED_TO_POSITION_MS, 1000L)
            }
            return null
        }
    }
}

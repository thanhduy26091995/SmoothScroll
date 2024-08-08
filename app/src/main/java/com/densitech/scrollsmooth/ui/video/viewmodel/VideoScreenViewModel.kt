package com.densitech.scrollsmooth.ui.video.viewmodel

import android.content.Context
import android.os.HandlerThread
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cronet.CronetDataSource
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
import com.densitech.scrollsmooth.ui.video.preFetch.BitrateManager
import com.densitech.scrollsmooth.ui.video.preFetch.CacheSingleton
import com.densitech.scrollsmooth.ui.video.preFetch.MediaItemSource
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.chromium.net.CronetEngine
import java.util.concurrent.Executors
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
    private val holderMap: MutableMap<Int, ExoPlayer> = mutableMapOf()
    private val holderRatioMap: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
    var currentPlayingIndex: Int = C.INDEX_UNSET
        private set

    companion object {
        private const val TAG = "VideoScreenViewModel"
        private const val LOAD_CONTROL_MIN_BUFFER_MS = 20_000
        private const val LOAD_CONTROL_MAX_BUFFER_MS = 20_000
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS = 2500
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_AFTER_RE_BUFFER = 1000
        private const val MANAGED_ITEM_COUNT = 5
        private const val ITEM_ADD_REMOVE_COUNT = 5
        private const val NUMBER_OF_PLAYERS = 7
    }

    private val playbackThread: HandlerThread =
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)

    private val urlList = listOf(
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_1.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_3.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_4.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_5.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_6.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_7.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_8.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_9.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_10.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_11.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_12.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_13.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_14.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_15.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_16.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_17.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_18.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_19.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_20.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_21.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_22.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_23.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_24.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_25.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_26.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_27.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_28.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_29.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_30.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_31.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_32.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_33.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_34.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_35.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_36.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_37.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_38.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_39.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_40.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_41.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/video_42.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/1_20240808181704_5512609-hd_1080_1920_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/2_20240808181708_9861969-uhd_1440_2732_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/3_20240808181711_8045175-hd_1080_1920_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/4_20240808181713_7414127-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/5_20240808181715_8032945-uhd_1440_2560_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/6_20240808181718_7515918-hd_1080_1920_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/7_20240808181721_5992585-uhd_1440_2560_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/8_20240808181724_8045109-hd_1080_1920_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/9_20240808181728_6077718-uhd_2560_1440_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/10_20240808181733_7612773-hd_1080_1920_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/11_20240808181737_7322355-uhd_1440_2732_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/12_20240808181744_5642526-hd_1080_1920_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/13_20240808181753_7181824-uhd_1440_2732_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/14_20240808181817_2169880-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/15_20240808181831_3015510-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/16_20240808181834_3135808-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/17_20240808181836_2252797-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/18_20240808181841_2519660-uhd_2560_1440_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/19_20240808181847_3018669-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/20_20240808181851_3629511-hd_720_900_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/21_20240808181859_2053855-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/22_20240808181911_1854202-hd_1280_720_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/23_20240808181921_2249630-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/24_20240808181928_2867873-uhd_2560_1440_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/25_20240808181934_3015527-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/26_20240808181941_2711092-uhd_2560_1440_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/27_20240808181946_3775278-hd_1920_1080_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/28_20240808181951_3120159-uhd_2560_1440_25fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/29_20240808182103_2146396-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/30_20240808182114_2103099-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/31_20240808182131_2933375-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/32_20240808182142_2547258-uhd_2560_1440_30fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/33_20240808182147_3059073-hd_1920_1080_24fps.mp4",
        "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/videos/34_20240808182151_1994829-hd_1920_1080_24fps.mp4"
    )

    fun initData(context: Context) {
        val mediaItems = arrayListOf<MediaItem>()
        for (i in urlList.indices) {
            val randomUrl = urlList.random()
            val mediaItem = MediaItem.Builder().setUri(randomUrl).setMediaId(i.toString()).build()
            mediaItems.add(mediaItem)
        }

        _playList.value = mediaItems
        _mediaItemSource.value = MediaItemSource(_playList.value)

        playbackThread.start()
        val renderersFactory = DefaultRenderersFactory(context)
        val loadControl =
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    LOAD_CONTROL_MIN_BUFFER_MS,
                    LOAD_CONTROL_MAX_BUFFER_MS,
                    LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS,
                    LOAD_CONTROL_BUFFER_FOR_PLAYBACK_AFTER_RE_BUFFER
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

        val cache = CacheSingleton.getInstance(context)
        val cacheSink = CacheDataSink.Factory().setCache(cache)

        // Cronet
        val cronetEngine = CronetEngine.Builder(context).build()
        val cronetDataSourceFactory = CronetDataSource.Factory(
            cronetEngine,
            Executors.newSingleThreadExecutor()
        )

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setUpstreamDataSourceFactory(cronetDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or CacheDataSource.FLAG_BLOCK_ON_CACHE)

        preloadManager = DefaultPreloadManager(
            DefaultPreloadControl(),
            DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory),
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
                return DefaultPreloadManager.Status(STAGE_LOADED_TO_POSITION_MS, 1000L)
            } else if (abs(rankingData - currentPlayingIndex) == 1) {
                return DefaultPreloadManager.Status(STAGE_LOADED_TO_POSITION_MS, 1000L)
            }
            return null
        }
    }
}

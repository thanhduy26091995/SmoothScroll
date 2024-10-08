package com.densitech.scrollsmooth.ui.video.viewmodel

import android.content.Context
import android.os.Bundle
import android.os.HandlerThread
import android.os.Process
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRendererCapabilitiesList
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager.Status.STAGE_LOADED_TO_POSITION_MS
import androidx.media3.exoplayer.source.preload.TargetPreloadStatusControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.densitech.scrollsmooth.ui.downloader.DownloadManagerSingleton
import com.densitech.scrollsmooth.ui.downloader.DownloadServiceHelper
import com.densitech.scrollsmooth.ui.downloader.DownloadVideoCache
import com.densitech.scrollsmooth.ui.video.model.MediaInfo
import com.densitech.scrollsmooth.ui.video.model.MediaSourceState
import com.densitech.scrollsmooth.ui.video.model.ScreenState
import com.densitech.scrollsmooth.ui.video.prefetch.CacheSingleton
import com.densitech.scrollsmooth.ui.video.prefetch.MediaItemSource
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chromium.net.CronetEngine
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.math.abs

@UnstableApi
@HiltViewModel
class VideoScreenViewModel @Inject constructor(private val getVideosUseCase: GetVideosUseCase = GetVideosUseCase()) :
    ViewModel(), DownloadServiceHelper.Listener {
    private val _playList: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())

    private val _playerPool: MutableStateFlow<PlayerPool?> = MutableStateFlow(null)
    val playerPool = _playerPool.asStateFlow()

    private val _mediaItemSource: MutableStateFlow<MediaItemSource?> = MutableStateFlow(null)
    val mediaItemSource = _mediaItemSource.asStateFlow()

    private val _screenState: MutableStateFlow<ScreenState> =
        MutableStateFlow(ScreenState.LOADING_STATE)
    val screenState = _screenState.asStateFlow()

    private val _mediaSourceState: MutableStateFlow<MediaSourceState> =
        MutableStateFlow(MediaSourceState.REMOTE_SOURCE)
    val mediaSourceState = _mediaSourceState.asStateFlow()

    private val _videoDownloadedList: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val videoDownloadedList = _videoDownloadedList.asStateFlow()

    private lateinit var preloadManager: DefaultPreloadManager
    private val currentMediaItemsAndIndexes: ArrayDeque<Pair<MediaItem, Int>> = ArrayDeque()
    private val holderMap: MutableMap<Int, ExoPlayer> = mutableMapOf()
    private val holderRatioMap: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
    var currentPlayingIndex: Int = C.INDEX_UNSET
        private set

    private val playbackThread: HandlerThread =
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)

    private lateinit var downloadServiceHelper: DownloadServiceHelper

    companion object {
        private const val TAG = "VideoScreenViewModel"
        private const val LOAD_CONTROL_MIN_BUFFER_MS = 20_000
        private const val LOAD_CONTROL_MAX_BUFFER_MS = 20_000
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_MS = 2500
        private const val LOAD_CONTROL_BUFFER_FOR_PLAYBACK_AFTER_RE_BUFFER = 1000
        private const val MANAGED_ITEM_COUNT = 5
        private const val ITEM_ADD_REMOVE_COUNT = 5
        private const val NUMBER_OF_PLAYERS = 7
        const val MAX_DURATION_TIME_TO_SEEK = 15000
        const val EXTRAS_METADATA = "metadata"
        const val CACHED_DOWNLOAD_FOLDER = "video_cached"
        const val DOWNLOAD_FOLDER = "video_downloaded"
    }

    init {
        viewModelScope.launch {
            val remoteVideoList = getRemoteVideos()
            if (remoteVideoList.isEmpty()) {
                _screenState.value = ScreenState.OFFLINE_REQUEST_STATE
                return@launch
            }

            buildMediaItemList(remoteVideoList)
        }
    }

    fun initData(context: Context) {
        _mediaItemSource.value = MediaItemSource(emptyList())

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

        // Init download
        downloadServiceHelper =
            DownloadServiceHelper(
                context,
                cronetDataSourceFactory,
                DownloadManagerSingleton.getInstance(context)
            )
        _videoDownloadedList.value = downloadServiceHelper.getDownloadedVideo().map { it.videoUrl }
    }

    fun downloadVideo(index: Int) {
        val mediaItem = _mediaItemSource.value?.mediaItems?.get(index) ?: return
        downloadServiceHelper.downloadClick(mediaItem)
    }

    fun getMediaSourceByMediaItem(
        context: Context,
        mediaItem: MediaItem,
        index: Int
    ): MediaSource? {
        if (_mediaSourceState.value == MediaSourceState.LOCAL_SOURCE) {
            val cache = DownloadVideoCache.getInstance(context)
            val cronetEngine = CronetEngine.Builder(context).build()
            val cronetDataSourceFactory = CronetDataSource.Factory(
                cronetEngine,
                Executors.newSingleThreadExecutor()
            )

            val cacheDataSourceFactory: DataSource.Factory =
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(cronetDataSourceFactory)
                    .setCacheWriteDataSinkFactory(null) // Disable writing.

            return ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                .createMediaSource(mediaItem)
        }
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
        // Only change screen state in case video 0 is loaded with a size
        if (token == 0 && width > 0 && height > 0) {
            _screenState.value = ScreenState.PLAY_STATE
        }
        holderRatioMap[token] = Pair(width, height)
    }

    fun getCurrentMediaInfo(mediaMetadata: MediaMetadata): MediaInfo {
        val mediaInfoStr = mediaMetadata.extras?.getString(EXTRAS_METADATA)
            ?: throw Exception("MediaInfo not found")
        val mediaInfo = Json.decodeFromString<MediaInfo>(mediaInfoStr)
        return mediaInfo
    }

    fun getCurrentRatio(token: Int, mediaMetadata: MediaMetadata): Pair<Int, Int> {
        val currentValue = holderRatioMap[token]
        if (currentValue == null) {
            val configRatio = mediaMetadata.extras?.let {
                val metadataStr = it.getString(EXTRAS_METADATA) ?: return@let null
                val metadata =
                    Json.decodeFromString<com.densitech.scrollsmooth.ui.video.model.MediaMetadata>(
                        metadataStr
                    )

                val width = metadata.width.toInt()
                val height = metadata.height.toInt()
                Pair(width, height)
            }

            return configRatio ?: Pair(0, 0)
        }
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

    fun pauseAllPlayer() {
        holderMap.values.forEach {
            it.pause()
        }
    }

    fun play(position: Int) {
        currentPlayingIndex = position
        holderMap[position]?.let {
            _playerPool.value?.play(it)
        }
        preloadManager.setCurrentPlayingIndex(currentPlayingIndex)
        preloadManager.invalidate()
    }

    fun loadRemoteVideoList() {
        viewModelScope.launch {
            _screenState.value = ScreenState.LOADING_STATE
            val remoteVideoList = getRemoteVideos()
            if (remoteVideoList.isEmpty()) {
                _screenState.value = ScreenState.OFFLINE_REQUEST_STATE
                return@launch
            }

            buildMediaItemList(remoteVideoList)
        }
    }

    fun loadDownloadedVideoList() {
        viewModelScope.launch {
            _screenState.value = ScreenState.LOADING_STATE
            _mediaSourceState.value = MediaSourceState.LOCAL_SOURCE
            val remoteVideoList = getDownloadedVideos()
            buildMediaItemList(remoteVideoList)
        }
    }

    fun registerDownloadState() {
        downloadServiceHelper.addListener(this)
    }

    fun unRegisterDownloadState() {
        downloadServiceHelper.removeListener(this)
    }

    private fun addMediaItem(index: Int, isAddingToRight: Boolean) {
        val mediaItems = mediaItemSource.value?.mediaItems ?: return
        if (index < 0 || mediaItems.isEmpty()) {
            return
        }

        val mediaItem = mediaItems[index]
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

    private fun getDownloadedVideos(): List<MediaInfo> {
        return downloadServiceHelper.getDownloadedVideo()
    }

    private suspend fun getRemoteVideos(): List<MediaInfo> {
        return getVideosUseCase.fetchVideos()
    }

    private fun buildMediaItemList(videoList: List<MediaInfo>) {
        val mediaItems = arrayListOf<MediaItem>()
        for (video in videoList) {
            val metaData = MediaMetadata.Builder()
                .setTitle(video.title)
                .setExtras(Bundle().apply {
                    putString(EXTRAS_METADATA, Json.encodeToString(video))
                }).build()
            val mediaItem =
                MediaItem.Builder().setUri(video.videoUrl)
                    .setMediaMetadata(metaData)
                    .setMediaId(video.videoUrl).build()
            mediaItems.add(mediaItem)
        }

        val updatedList = _playList.value.toMutableList().apply {
            addAll(mediaItems)
        }
        _playList.value = updatedList
        _mediaItemSource.value = MediaItemSource(_playList.value)
        _screenState.value = ScreenState.BUFFER_STATE
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

    override fun onDownloadCompleted(videoId: String) {
        val updatedList = _videoDownloadedList.value.toMutableList().apply {
            if (!contains(videoId)) {
                add(videoId)
            }
        }
        _videoDownloadedList.value = updatedList
    }

    override fun onDownloadRemoved(videoId: String) {
        val updatedList = _videoDownloadedList.value.toMutableList().apply {
            remove(videoId)
        }
        _videoDownloadedList.value = updatedList
    }
}

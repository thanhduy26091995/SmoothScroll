package com.densitech.scrollsmooth.ui.video_transformation.viewmodel

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams
import com.densitech.scrollsmooth.ui.utils.NUMBER_OF_FRAME_ITEM
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalThumbnail
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/*
* @Author: Dennis
* @Description: View model to control for video transformation screen, its contains some feature
* 1. Process and load thumbnails
* 2. Handle trim range for video
* 3. Control playing state
* 4. Handle current position
* 5. Handle text overlay
* 6. Control exo player from view
* */
@HiltViewModel
class VideoTransformationViewModel @Inject constructor(

) : ViewModel() {
    private val _thumbnails: MutableStateFlow<List<DTOLocalThumbnail>> =
        MutableStateFlow(emptyList())
    val thumbnails = _thumbnails.asStateFlow()

    private val _trimRange: MutableStateFlow<LongRange> = MutableStateFlow(0L..0L)
    val trimmedRangeSelected = _trimRange.asStateFlow()

    private val _currentPosition: MutableStateFlow<Long> = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    // For preview
    val trimmedHandler = Handler(Looper.getMainLooper())

    private val _isPlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _textOverlayList: MutableStateFlow<List<TextOverlayParams>> =
        MutableStateFlow(emptyList())
    val textOverlayList = _textOverlayList.asStateFlow()

    // ExoPlayer instance pass from view (Be careful with the lifecycle)
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    fun releaseData() {
        _exoPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
        _trimRange.value = LongRange(0, 0)
        _thumbnails.value = emptyList()
    }

    fun setIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun setExoPlayer(player: ExoPlayer) {
        _exoPlayer = player
    }

    fun onVideoEnded() {
        pauseVideo()
        setIsPlaying(false)
        _currentPosition.value = _trimRange.value.first
    }

    fun pauseVideo() {
        // Pause a handler
        trimmedHandler.removeCallbacks(checkPositionHandler)
        _exoPlayer?.pause()
    }

    fun playVideo() {
        val seekPosition = when {
            _currentPosition.value >= _trimRange.value.last -> _trimRange.value.first
            _currentPosition.value <= _trimRange.value.first -> _trimRange.value.first
            else -> _currentPosition.value
        }

        _currentPosition.value = seekPosition
        // Register handler to loop every seconds
        trimmedHandler.post(checkPositionHandler)
        // Playing
        _exoPlayer?.seekTo(seekPosition)
        _exoPlayer?.play()
    }

    fun updateCurrentPosition(position: Long) {
        _currentPosition.value = position
    }

    fun updateTrimRange(first: Long, last: Long) {
        _trimRange.value = first..last
    }

    fun extractThumbnailsPerSecond(selectedVideo: DTOLocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            val thumbnails = mutableListOf<DTOLocalThumbnail>()
            val videoDuration = selectedVideo.duration.toLong() / 1000
            val stepPerSeconds = videoDuration / (NUMBER_OF_FRAME_ITEM * 1.0)
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(selectedVideo.videoPath)

            var previousThumbnail: Bitmap? = null

            for (i in 1..NUMBER_OF_FRAME_ITEM) {
                val timeAtMs = (i * stepPerSeconds * 1000000L).toLong() // Convert to microseconds
                val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    retriever.getScaledFrameAtTime(
                        timeAtMs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        50,
                        100
                    )
                } else {
                    retriever.getFrameAtTime(timeAtMs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }

                if (thumbnail != null && thumbnail != previousThumbnail) {
                    val localThumbnail =
                        DTOLocalThumbnail(thumbnail = thumbnail, timestamp = timeAtMs / 1000)
                    thumbnails.add(localThumbnail)
                    previousThumbnail = thumbnail
                }
            }

            withContext(Dispatchers.Main) {
                _thumbnails.value = thumbnails
            }
        }
    }

    fun addNewTextOverlay(params: TextOverlayParams) {
        val currentList = _textOverlayList.value.toMutableList()
        currentList.add(params)
        _textOverlayList.value = currentList
    }

    fun removeTextOverlayById(key: String) {
        val currentList = _textOverlayList.value.toMutableList()
        currentList.removeAll { it.key == key }
        _textOverlayList.value = currentList
    }

    fun onTransformGestureChanged(key: String, pan: Offset, zoom: Float, rotation: Float) {
        val selectedOverlay = _textOverlayList.value.find { it.key == key } ?: return
        val currentZoom = (zoom).coerceIn(0.5f, 5f)
        // Update overlay
        val newOverlay = selectedOverlay.copy(
            scale = currentZoom,
            textX = pan.x,
            textY = pan.y,
            rotationAngle = rotation
        )
        val currentList = _textOverlayList.value.toMutableList()
        currentList[currentList.indexOf(selectedOverlay)] = newOverlay
        _textOverlayList.value = currentList
    }

    private val checkPositionHandler = object : Runnable {
        override fun run() {
            val newPlayingPosition = _exoPlayer?.currentPosition ?: return
            if (newPlayingPosition >= _trimRange.value.last) {
                // Reach end of trimmed, we will pause the video, then seek to first
                _currentPosition.value = _trimRange.value.first
                pauseVideo()
                setIsPlaying(false)
            } else {
                _currentPosition.value = newPlayingPosition
                trimmedHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
    }
}
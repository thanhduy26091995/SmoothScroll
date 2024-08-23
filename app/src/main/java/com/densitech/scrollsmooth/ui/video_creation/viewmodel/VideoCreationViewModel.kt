package com.densitech.scrollsmooth.ui.video_creation.viewmodel

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VideoCreationViewModel @Inject constructor() : ViewModel() {

    private val _localVideos: MutableStateFlow<List<DTOLocalVideo>> = MutableStateFlow(emptyList())
    val localVideos = _localVideos.asStateFlow()

    private val _selectedVideo: MutableStateFlow<DTOLocalVideo?> = MutableStateFlow(null)
    val selectedVideo = _selectedVideo.asStateFlow()

    private val _isProcessedThumbnail: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isProcessedThumbnail = _isProcessedThumbnail.asStateFlow()

    // Init caching
    private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    val videoCachingThumbnail = VideoThumbnailCache(cacheSize)

    fun getAllVideos(context: Context) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.RESOLUTION
        )

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )

        val localVideos = arrayListOf<DTOLocalVideo>()

        cursor?.use {
            val idColumn = it.getColumnIndex(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = it.getColumnIndex(MediaStore.Video.Media.DATA)
            val durationColumn = it.getColumnIndex(MediaStore.Video.Media.DURATION)
            val resolutionColumn = it.getColumnIndex(MediaStore.Video.Media.RESOLUTION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val dataStr = it.getString(dataColumn)
                val duration = it.getString(durationColumn)
                val resolution = it.getString(resolutionColumn)
                val width = resolution.split("×").first()
                val height = resolution.split("×").last()

                localVideos.add(
                    DTOLocalVideo(
                        id = id,
                        videoName = name,
                        videoPath = dataStr,
                        duration = duration,
                        width = width.toInt(),
                        height = height.toInt()
                    )
                )
            }
        }

        _localVideos.value = localVideos
        processThumbnail()
        if (localVideos.isNotEmpty() && _selectedVideo.value == null) {
            _selectedVideo.value = localVideos.first()
        }
    }

    fun onVideoClick(data: DTOLocalVideo) {
        _selectedVideo.value = data
    }

    fun extractFramesFromVideo(context: Context, videoPath: String) {
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(videoPath)

        var videoTrackIndex = -1
        for (i in 0 until mediaExtractor.trackCount) {
            val format = mediaExtractor.getTrackFormat(i)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
            if (mimeType?.startsWith("video/") == true) {
                videoTrackIndex = i
                mediaExtractor.selectTrack(videoTrackIndex)
                break
            }
        }

        if (videoTrackIndex == -1) {
            // No video track found
            return
        }

        val format = mediaExtractor.getTrackFormat(videoTrackIndex)
        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return

        val mediaCodec = MediaCodec.createDecoderByType(mimeType)
        mediaCodec.configure(format, null, null, 0)
        mediaCodec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var frameCount = 0

        while (true) {
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        mediaCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        break
                    } else {
                        mediaCodec.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            sampleSize,
                            mediaExtractor.sampleTime,
                            0
                        )
                        mediaExtractor.advance()
                    }
                }
            }

            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex >= 0) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null) {
                    val frameData = ByteArray(bufferInfo.size)
                    outputBuffer.get(frameData)

                    // Process the frame (e.g., save to file, display, etc.)
                    processFrame(frameData, frameCount)
                    frameCount++
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // Output buffers changed, nothing to do here
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Output format changed, handle this if necessary
            }
        }

        mediaCodec.stop()
        mediaCodec.release()
        mediaExtractor.release()
    }

    private fun processFrame(frameData: ByteArray, frameCount: Int) {
        println("PROCESS FRAME with: ${frameCount}")
    }

    // Process thumbnail will take time, so we need to separate it as different process
    private fun processThumbnail() {
        viewModelScope.launch(Dispatchers.IO) {
            for (video in _localVideos.value) {
                // Check if the thumbnail is in the cache
                var thumbnail = videoCachingThumbnail[video.videoPath]
                if (thumbnail == null) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(video.videoPath)
                    thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        retriever.getScaledFrameAtTime(
                            1000,
                            MediaMetadataRetriever.OPTION_CLOSEST,
                            512,
                            512
                        )
                    } else {
                        retriever.frameAtTime
                    }

                    if (thumbnail != null) {
                        videoCachingThumbnail.put(video.videoPath, thumbnail)
                    }
                    retriever.release()
                }
            }

            // Switch to main thread to update UI
            withContext(Dispatchers.Main) {
                _isProcessedThumbnail.value = true
            }
        }
    }
}
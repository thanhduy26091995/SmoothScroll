package com.densitech.scrollsmooth.ui.video_transformation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class VideoTransformationViewModel @Inject constructor() : ViewModel() {
    private val thumbnailList = arrayListOf<Bitmap>()

    fun updateThumbnailList(thumbnails: List<Bitmap>) {
        thumbnailList.clear()
        thumbnailList.addAll(thumbnails)
    }

    fun getThumbnailList(): List<Bitmap> {
        return thumbnailList
    }

    suspend fun extractThumbnailsPerSecond(videoPath: String): List<Bitmap> {
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
            return emptyList()
        }

        val format = mediaExtractor.getTrackFormat(videoTrackIndex)
        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
        val duration = format.getLong(MediaFormat.KEY_DURATION) / 1000000 // convert to seconds

        val mediaCodec = MediaCodec.createDecoderByType(mimeType)
        mediaCodec.configure(format, null, null, 0)
        mediaCodec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val thumbnails = mutableListOf<Bitmap>()

        for (second in 0 until duration.toInt()) {
            // Seek to the time position at this second
            mediaExtractor.seekTo(second * 1000000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                if (inputBuffer != null) {
                    val sampleSize = mediaExtractor.readSampleData(inputBuffer, 0)
                    if (sampleSize >= 0) {
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

                    // Convert frame data to Bitmap
                    val bitmap = convertYUV420ToBitmap(format, frameData)
                    if (bitmap != null) {
                        thumbnails.add(bitmap)
                    }
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

        println(thumbnails.size)
        return thumbnails
    }


    fun convertYUV420ToBitmap(format: MediaFormat, frameData: ByteArray): Bitmap? {
        val width = format.getInteger(MediaFormat.KEY_WIDTH)
        val height = format.getInteger(MediaFormat.KEY_HEIGHT)

        val ySize = width * height
        val uvSize = ySize / 4

        val y = frameData.copyOfRange(0, ySize)
        val u = frameData.copyOfRange(ySize, ySize + uvSize)
        val v = frameData.copyOfRange(ySize + uvSize, ySize + 2 * uvSize)

        val intArray = IntArray(width * height)
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIndex = j * width + i

                // YUV420P to RGB conversion
                val Y = y[yIndex].toInt() and 0xff
                val U = u[(j / 2) * (width / 2) + (i / 2)].toInt() and 0xff
                val V = v[(j / 2) * (width / 2) + (i / 2)].toInt() and 0xff

                // Conversion formula
                val R = (Y + 1.370705f * (V - 128)).toInt()
                val G = (Y - 0.337633f * (U - 128) - 0.698001f * (V - 128)).toInt()
                val B = (Y + 1.732446f * (U - 128)).toInt()

                // Clipping RGB values to 8-bit range
                intArray[yIndex] = (0xff000000.toInt()
                        or ((R.coerceIn(0, 255)) shl 16)
                        or ((G.coerceIn(0, 255)) shl 8)
                        or (B.coerceIn(0, 255)))
            }
        }

        return Bitmap.createBitmap(intArray, width, height, Bitmap.Config.ARGB_8888)
    }

}
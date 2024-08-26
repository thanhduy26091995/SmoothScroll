package com.densitech.scrollsmooth.ui.video_transformation

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class VideoTransformationViewModel @Inject constructor() : ViewModel() {
    private val _thumbnails: MutableStateFlow<List<DTOLocalThumbnail>> =
        MutableStateFlow(emptyList())
    val thumbnails = _thumbnails.asStateFlow()

    fun extractThumbnailsPerSecond(selectedVideo: DTOLocalVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            val thumbnails = mutableListOf<DTOLocalThumbnail>()
            val videoDuration = selectedVideo.duration.toLong() / 1000
            val stepPerSeconds = videoDuration / NUMBER_OF_FRAME_ITEM * 1.0
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
                println(thumbnails.size)
                _thumbnails.value = thumbnails
            }
        }
    }
}
package com.densitech.scrollsmooth.ui.video

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VideoScreenViewModel @Inject constructor() : ViewModel() {
    private val _playList: MutableStateFlow<List<MediaItem>> = MutableStateFlow(listOf())
    val playList: StateFlow<List<MediaItem>> = _playList

    init {
        _playList.value = listOf(
            MediaItem.fromUri("https://bestvpn.org/html5demos/assets/dizzy.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-1/gen-3/screens/dash-vod-single-segment/video-avc-baseline-480.mp4"),
            MediaItem.fromUri("https://html5demos.com/assets/dizzy.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_1.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_2.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_3.mp4"),
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/shortform_4.mp4"),
        )
    }
}

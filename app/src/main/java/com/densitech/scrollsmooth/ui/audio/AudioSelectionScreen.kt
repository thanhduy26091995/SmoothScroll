package com.densitech.scrollsmooth.ui.audio

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel

@Composable
fun AudioSelectionScreen(videoTransformationViewModel: VideoTransformationViewModel) {
    val audios by videoTransformationViewModel.audios.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(audios) {
            AudioItemView(audioResponse = it, onActionClick = {

            }, onSelectAudio = {

            })
        }
    }
}
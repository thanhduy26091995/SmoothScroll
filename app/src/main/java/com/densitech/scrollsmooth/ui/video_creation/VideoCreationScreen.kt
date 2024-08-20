package com.densitech.scrollsmooth.ui.video_creation

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun VideoCreationScreen(viewModel: VideoCreationViewModel) {
    val context = LocalContext.current

    val localVideos by viewModel.localVideos.collectAsState()

    LaunchedEffect(true) {
        viewModel.getAllVideos(context)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(localVideos.toList()) {
            VideoCreationItemView(
                data = it,
                thumbnail = viewModel.videoCachingThumbnail[it.videoPath],
                modifier =  Modifier.aspectRatio(1f)
            )
        }
    }
}
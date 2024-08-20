package com.densitech.scrollsmooth.ui.video_creation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun VideoCreationScreen(viewModel: VideoCreationViewModel) {
    val context = LocalContext.current

    LaunchedEffect(true) {
        viewModel.getAllVideos(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
    ) {

    }
}
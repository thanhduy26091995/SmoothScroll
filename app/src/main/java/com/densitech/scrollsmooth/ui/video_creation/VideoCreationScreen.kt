package com.densitech.scrollsmooth.ui.video_creation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCreationScreen(viewModel: VideoCreationViewModel) {
    val context = LocalContext.current

    val localVideos by viewModel.localVideos.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()

    LaunchedEffect(true) {
        viewModel.getAllVideos(context)
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(text = "New Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }, navigationIcon = {
            IconButton(onClick = {

            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        })
    }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val screenHeight = maxHeight
            val previewHeight = screenHeight / 4 * 2

            CollapsingLayout(
                collapsingTop = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth()
                            .height(previewHeight)
                            .background(Color.Black)
                    ) {
                        selectedVideo?.let {
                            VideoSelectedView(
                                data = it
                            )
                        }
                    }
                }, bodyContent = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        items(localVideos.toList()) {
                            VideoCreationItemView(
                                data = it,
                                thumbnail = viewModel.videoCachingThumbnail[it.videoPath],
                                onVideoClick = { video ->
                                    viewModel.onVideoClick(video)
                                },
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                    }
                }, modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}
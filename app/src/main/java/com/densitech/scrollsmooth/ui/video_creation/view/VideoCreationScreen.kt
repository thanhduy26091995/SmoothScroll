package com.densitech.scrollsmooth.ui.video_creation.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.densitech.scrollsmooth.ui.main.Screen
import com.densitech.scrollsmooth.ui.utils.CollapsingLayout
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCreationScreen(navController: NavController, viewModel: VideoCreationViewModel) {
    val context = LocalContext.current

    val localVideos by viewModel.localVideos.collectAsState()
    val selectedVideo by viewModel.selectedVideo.collectAsState()
    val isProcessedThumbnail by viewModel.isProcessedThumbnail.collectAsState()

    LaunchedEffect(true) {
        viewModel.getAllVideos(context)
    }

    Scaffold(topBar = {
        TopAppBar(title = {
            Text(text = "New Post", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }, navigationIcon = {
            IconButton(onClick = {
                navController.navigate(Screen.Home.route)
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }, actions = {
            TextButton(onClick = {
                navController.navigate(Screen.VideoTransformation.route)
            }) {
                Text(text = "Next")
            }
        })
    }) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val screenHeight = maxHeight
            val previewHeight = screenHeight / 2

            CollapsingLayout(
                collapsingTop = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .fillMaxWidth()
                            .height(previewHeight)
                    ) {
                        selectedVideo?.let {
                            VideoSelectedView(
                                data = it,
                                modifier = Modifier.fillMaxSize()
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
                                isSelected = selectedVideo?.id == it.id,
                                thumbnail = if (isProcessedThumbnail) viewModel.videoCachingThumbnail[it.videoPath] else null,
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
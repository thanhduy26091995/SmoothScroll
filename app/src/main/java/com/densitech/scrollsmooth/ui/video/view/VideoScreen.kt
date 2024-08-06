@file:OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {

    val context = LocalContext.current
    val mediaItemSource = videoScreenViewModel.mediaItemSource.collectAsState()
    val playerPool = videoScreenViewModel.playerPool.collectAsState()
    var currentActiveIndex by remember {
        mutableIntStateOf(videoScreenViewModel.currentPlayingIndex)
    }

    val mediaList = remember {
        mutableStateListOf<MediaItem>()
    }

    val pagerState = rememberPagerState(
        pageCount = {
            1000
        },
        initialPage = 0
    )

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.3f
    )

    LaunchedEffect(true) {
        if (mediaItemSource.value?.mediaItems.isNullOrEmpty()) {
            videoScreenViewModel.initData(context)
        }
    }

    LaunchedEffect(mediaItemSource.value?.mediaItems) {
        val mediaItems = mediaItemSource.value?.mediaItems
        if (mediaItems != null) {
            mediaList.clear()
            mediaList.addAll(mediaItems)

            // Run the first item after delay 500ms
            delay(500)
            videoScreenViewModel.play(0)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage
        }.distinctUntilChanged().collect { page ->
            if (mediaList.isNotEmpty()) {
                val realPage = page % mediaList.count()
                currentActiveIndex = realPage
                videoScreenViewModel.play(realPage)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeight = maxHeight
        // Compute the total page count outside of the VerticalPager composable
        if (mediaList.size > 0) {
            val totalPageCount = remember(mediaList) { mediaList.size }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1,
                flingBehavior = fling
            ) { page ->
                val realPage = page % totalPageCount
                val mediaItem = mediaList.getOrNull(realPage) ?: return@VerticalPager
                val mediaSource =
                    videoScreenViewModel.getMediaSourceByMediaItem(mediaItem, realPage)
                        ?: return@VerticalPager

                // Ensure playerPool.value is not null
                val currentPlayerPool = playerPool.value ?: return@VerticalPager

                VideoItemView(
                    playerPool = currentPlayerPool,
                    isActive = currentActiveIndex == realPage,
                    currentToken = realPage,
                    currentMediaSource = mediaSource,
                    currentRatio = videoScreenViewModel.getCurrentRatio(realPage),
                    onPlayerReady = { token, exoPlayer ->
                        videoScreenViewModel.onPlayerReady(token, exoPlayer)
                    },
                    onPlayerDestroy = { token ->
                        videoScreenViewModel.onPlayerDestroy(token)
                    },
                    onReceiveRatio = { token, width, height ->
                        videoScreenViewModel.onReceiveRatio(token, width, height)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}
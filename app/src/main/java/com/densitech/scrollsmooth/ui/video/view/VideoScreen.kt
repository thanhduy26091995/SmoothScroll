@file:OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.ui.video.model.ScreenState
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(pagerState: PagerState, videoScreenViewModel: VideoScreenViewModel) {

    val context = LocalContext.current
    val mediaItemSource = videoScreenViewModel.mediaItemSource.collectAsState()
    val playerPool = videoScreenViewModel.playerPool.collectAsState()
    val screenState = videoScreenViewModel.screenState.collectAsState()
    var currentActiveIndex by remember {
        mutableIntStateOf(videoScreenViewModel.currentPlayingIndex)
    }

    val mediaList = remember {
        mutableStateListOf<MediaItem>()
    }

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1),
        snapPositionalThreshold = 0.3f,
        snapAnimationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    LaunchedEffect(true) {
        videoScreenViewModel.pauseAllPlayer()
        if (mediaItemSource.value?.mediaItems.isNullOrEmpty()) {
            videoScreenViewModel.initData(context)
        }
    }

    LaunchedEffect(mediaItemSource.value?.mediaItems) {
        val mediaItems = mediaItemSource.value?.mediaItems
        if (mediaItems != null) {
            mediaList.clear()
            mediaList.addAll(mediaItems)
        }
    }

    LaunchedEffect(screenState.value) {
        if (screenState.value == ScreenState.PLAY_STATE) {
            val playIndex = if (videoScreenViewModel.currentPlayingIndex == -1) {
                0
            } else {
                videoScreenViewModel.currentPlayingIndex
            }
            videoScreenViewModel.play(playIndex)
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

            Box {
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
                    val configRatio = mediaItem.mediaMetadata.extras?.let {
                        val width = it.getDouble("width").toInt()
                        val height = it.getDouble("height").toInt()
                        Pair(width, height)
                    }
                    val currentRatio = videoScreenViewModel.getCurrentRatio(realPage, configRatio)

                    VideoItemView(
                        playerPool = currentPlayerPool,
                        isActive = currentActiveIndex == realPage,
                        currentToken = realPage,
                        currentMediaSource = mediaSource,
                        currentRatio = currentRatio,
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
}
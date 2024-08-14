@file:OptIn(ExperimentalFoundationApi::class, ExperimentalPermissionsApi::class)

package com.densitech.scrollsmooth.ui.video.view

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.video.model.ScreenState
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("InlinedApi")
@ExperimentalPermissionsApi
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
    var isPause by remember {
        mutableStateOf(false)
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

    val notificationPermission = rememberPermissionState(
        permission = android.Manifest.permission.POST_NOTIFICATIONS,
        onPermissionResult = { isGrant ->
            // Handle permission result
        }
    )

    val lifeCycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Play latest video if needed
                val currentPlayingIndex = videoScreenViewModel.currentPlayingIndex
                if (currentPlayingIndex == -1) {
                    return@LifecycleEventObserver
                }
                videoScreenViewModel.play(currentPlayingIndex)
            } else if (event == Lifecycle.Event.ON_STOP) {
                videoScreenViewModel.pauseAllPlayer()
            } else if (event == Lifecycle.Event.ON_CREATE) {
                videoScreenViewModel.pauseAllPlayer()
                if (mediaItemSource.value?.mediaItems.isNullOrEmpty()) {
                    videoScreenViewModel.initData(context)
                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
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

                isPause = false
            }
        }
    }

    println(screenState.value)
    when (screenState.value) {
        ScreenState.LOADING_STATE -> {
            LoadingScreen()
        }

        ScreenState.BUFFER_STATE, ScreenState.PLAY_STATE -> {
            if (mediaList.size > 0) {
                val totalPageCount = remember(mediaList) { mediaList.size }

                Box {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
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
                        val mediaInfo =
                            videoScreenViewModel.getCurrentMediaInfo(mediaItem.mediaMetadata)

                        VideoItemView(
                            playerPool = currentPlayerPool,
                            isActive = currentActiveIndex == realPage,
                            currentToken = realPage,
                            currentMediaSource = mediaSource,
                            mediaInfo = mediaInfo,
                            isDownloaded = videoScreenViewModel.isVideoDownloaded(mediaItem),
                            onPlayerReady = { token, exoPlayer ->
                                videoScreenViewModel.onPlayerReady(token, exoPlayer)
                            },
                            onPlayerDestroy = { token ->
                                videoScreenViewModel.onPlayerDestroy(token)
                            },
                            onReceiveRatio = { token, width, height ->
                                videoScreenViewModel.onReceiveRatio(token, width, height)
                            },
                            onPauseClick = {
                                isPause = it
                            },
                            onDownloadVideoClick = { token ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // Handle download video here, but need to check permission first
                                    if (!notificationPermission.status.isGranted) {
                                        notificationPermission.launchPermissionRequest()
                                    } else {
                                        // Start to download
                                        videoScreenViewModel.downloadVideo(token)
                                    }
                                } else {
                                    // Start to download
                                    videoScreenViewModel.downloadVideo(token)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }

                    AnimatedVisibility(
                        visible = isPause,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .align(Alignment.Center)
                                .alpha(0.2f),
                        )
                    }
                }
            }
        }

        ScreenState.OFFLINE_REQUEST_STATE -> {
            DialogConfirmSwitchOffline(
                dialogTitle = "Can't fetch video from network",
                dialogText = "Click 'Retry' to retry a network call, click 'Offline' to switch to Offlfine Mode",
                dialogConfirmText = "Offline",
                dialogDismissText = "Retry",
                onRetryClick = {
                    videoScreenViewModel.loadRemoteVideoList()
                },
                onSwitchOfflineClick = {
                    videoScreenViewModel.loadDownloadedVideoList()
                })
        }
    }
}
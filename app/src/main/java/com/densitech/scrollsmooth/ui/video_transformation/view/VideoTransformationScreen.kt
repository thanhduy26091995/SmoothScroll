@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.video_transformation.view

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetCollapsed
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetContent
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetExpanded
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import com.densitech.scrollsmooth.ui.utils.format
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoTransformationScreen(
    navController: NavController,
    videoCreationViewModel: VideoCreationViewModel,
    videoTransformationViewModel: VideoTransformationViewModel,
) {
    val thumbnails by videoTransformationViewModel.thumbnails.collectAsState()
    val selectedVideo by videoCreationViewModel.selectedVideo.collectAsState()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val localConfiguration = LocalConfiguration.current

    val exoPlayer =
        rememberExoPlayer(
            context = context,
            videoUri = Uri.parse(selectedVideo?.videoPath),
            onVideoStateChanged = {})

    val trimmedHandler = Handler(Looper.getMainLooper())

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    exoPlayer.play()
                }

                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.stop()
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            exoPlayer.release()
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Sub for padding and peek height
    val playerHeight = localConfiguration.screenHeightDp.dp - 72.dp - 50.dp
    val deviceWidth = localConfiguration.screenWidthDp.dp

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val offsetBottomSheet by remember(scaffoldState) {
        derivedStateOf {
            runCatching {
                scaffoldState.bottomSheetState.requireOffset()
            }.getOrDefault(0f)
        }
    }

    val progress = rememberSaveable { mutableFloatStateOf(0f) }
    val sheetOffset = rememberSaveable { mutableFloatStateOf(-1f) }
    var trimRange by remember {
        mutableStateOf(0L..selectedVideo!!.duration.toLong())
    }
    var isPlayingPreview by rememberSaveable {
        mutableStateOf(true)
    }
    var isDraggingTrim by rememberSaveable {
        mutableStateOf(false)
    }
    var currentPosition by rememberSaveable {
        mutableLongStateOf(0L)
    }
    var onSeekingPosition by rememberSaveable {
        mutableLongStateOf(0L)
    }

    val checkPositionHandler = object : Runnable {
        override fun run() {
            currentPosition = exoPlayer.currentPosition.coerceIn(trimRange.first, trimRange.last)
            if (currentPosition >= trimRange.last) {
                isPlayingPreview = false
            } else {
                trimmedHandler.postDelayed(this, 1000)
            }
        }
    }

    fun playTrimmedVideo(currentPosition: Long) {
        exoPlayer.seekTo(currentPosition)
        exoPlayer.play()
        // Use a Handler to check the player's position periodically
        trimmedHandler.post(checkPositionHandler)
    }

    fun pauseTrimmedVideo() {
        trimmedHandler.removeCallbacks(checkPositionHandler)
        exoPlayer.pause()
        currentPosition = 0
    }

    LaunchedEffect(onSeekingPosition) {
        if (onSeekingPosition > 0 && isDraggingTrim) {
            trimRange = LongRange(onSeekingPosition, trimRange.last)
            exoPlayer.seekTo(onSeekingPosition)
        }
    }

    LaunchedEffect(isDraggingTrim) {
        if (isDraggingTrim) {
            isPlayingPreview = false
        }
    }

    LaunchedEffect(selectedVideo) {
        selectedVideo?.let {
            videoTransformationViewModel.extractThumbnailsPerSecond(it)
        }
    }

    LaunchedEffect(isPlayingPreview) {
        if (isPlayingPreview) {
            val seekPosition = when {
                kotlin.math.abs(onSeekingPosition - trimRange.first) <= 100 -> trimRange.first
                kotlin.math.abs(onSeekingPosition - trimRange.last) <= 100 -> trimRange.first
                else -> onSeekingPosition
            }
            playTrimmedVideo(currentPosition = seekPosition)
        } else {
            pauseTrimmedVideo()
        }
    }

    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow { offsetBottomSheet }.collect { offset ->
            if (sheetOffset.floatValue == -1f) {
                sheetOffset.floatValue = offset
            }
            progress.floatValue = (1 - (offset / sheetOffset.floatValue)).format(4).toFloat()
        }
    }

    BottomSheetScaffold(
        sheetDragHandle = {

        },
        scaffoldState = scaffoldState,
        sheetContent = {
            SheetContent {
                SheetExpanded(
                    currentFraction = progress.floatValue,
                ) {
                    SheetExpandedContentView(
                        thumbnails = thumbnails,
                        selectedVideo = selectedVideo!!,
                        isVideoPlaying = isPlayingPreview,
                        currentPlayingPosition = currentPosition,
                        onPlayClick = {
                            isPlayingPreview = !isPlayingPreview
                        },
                        onTrimChange = { start, end ->
                            trimRange = LongRange(start, end)
                        },
                        onSeekChange = { position ->
                            onSeekingPosition = position
                        },
                        onDragStateChange = { isDragging ->
                            isDraggingTrim = isDragging
                        }
                    )
                }

                SheetCollapsed(
                    currentFraction = progress.floatValue
                ) {
                    SheetCollapsedActionView(
                        onEditVideoClick = {
                            scope.launch {
                                scaffoldState.bottomSheetState.expand()
                            }
                        },
                        onNextVideoClick = {
                            scope.launch {
                                scaffoldState.bottomSheetState.expand()
                            }
                        })
                }
            }
        },
        sheetPeekHeight = 72.dp
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(top = 50.dp)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (progress.floatValue > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }, modifier = Modifier
                        .padding(top = 16.dp, start = 10.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                }
            }

            MainPreviewContent(
                exoPlayer = exoPlayer,
                currentFraction = progress.floatValue,
                onBackClick = {
                    navController.popBackStack()
                },
                onActionClick = { action ->
                    when (action) {
                        TransformationAction.Music -> {

                        }

                        else -> {

                        }
                    }
                },
                modifier = Modifier
                    .width(deviceWidth * (1 - progress.floatValue))
                    .height(playerHeight * (1 - progress.floatValue))
                    .clip(RoundedCornerShape(24.dp))
                    .align(Alignment.TopCenter)
                    .then(
                        if (progress.floatValue > 0f) {
                            val alpha = (progress.floatValue + DEFAULT_FRACTION).coerceIn(0f, 1f)
                            Modifier.border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = alpha),
                                shape = RoundedCornerShape(24.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@UnstableApi
@Composable
fun rememberExoPlayer(
    context: Context,
    videoUri: Uri,
    onVideoStateChanged: (Int) -> Unit,
): ExoPlayer {
    return remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri(videoUri))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    onVideoStateChanged.invoke(playbackState)
                }
            })
        }
    }
}

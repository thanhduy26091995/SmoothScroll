@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.video_transformation.view

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.densitech.scrollsmooth.ui.audio.AudioSelectionBottomSheet
import com.densitech.scrollsmooth.ui.audio.AudioSelectionViewModel
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetCollapsed
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetContent
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetExpanded
import com.densitech.scrollsmooth.ui.text.TextOverlayPreview
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import com.densitech.scrollsmooth.ui.utils.format
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoTransformationScreen(
    navController: NavController,
    selectedVideo: DTOLocalVideo,
    videoTransformationViewModel: VideoTransformationViewModel,
    audioSelectionViewModel: AudioSelectionViewModel,
) {
    // Data state from viewmodel
    val thumbnails by videoTransformationViewModel.thumbnails.collectAsState()
    val trimmedRangeSelected by videoTransformationViewModel.trimmedRangeSelected.collectAsState()
    val currentPosition by videoTransformationViewModel.currentPosition.collectAsState()
    val isPlaying by videoTransformationViewModel.isPlaying.collectAsState()

    // Context
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val localConfiguration = LocalConfiguration.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    // Create exo instance
    val exoPlayer =
        rememberExoPlayer(
            context = context,
            videoUri = Uri.parse(selectedVideo.videoPath),
            onVideoStateChanged = { state ->
                if (state == Player.STATE_ENDED) {
                    videoTransformationViewModel.onVideoEnded()
                }
            })

    // Sub for padding and peek height
    val playerHeight = localConfiguration.screenHeightDp.dp - 72.dp - 50.dp
    val deviceWidth = localConfiguration.screenWidthDp.dp

    // Scaffold state to control bottom sheet
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // Offset of bottom sheet, it will had value from 0 to DEFAULT_FRACTION
    val offsetBottomSheet by remember(scaffoldState) {
        derivedStateOf {
            runCatching {
                scaffoldState.bottomSheetState.requireOffset()
            }.getOrDefault(0f)
        }
    }
    val progress = rememberSaveable { mutableFloatStateOf(0f) }
    val sheetOffset = rememberSaveable { mutableFloatStateOf(-1f) }

    // Range of trim, default will be from 0 to duration
    var trimRange by remember {
        if (trimmedRangeSelected.first == 0L && trimmedRangeSelected.last == 0L) {
            mutableStateOf(0..selectedVideo.duration.toLong())
        } else {
            mutableStateOf(trimmedRangeSelected.first..trimmedRangeSelected.last)
        }

    }

    // Is Dragging trim, center line
    var isDraggingTrim by rememberSaveable {
        mutableStateOf(false)
    }

    // Flag to control bottom sheet audio
    var isShowAudioBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }

    // Flag to control text overlay
    var isShowTextingOverlay by rememberSaveable {
        mutableStateOf(false)
    }

    // Save exo player instance to viewmodel
    LaunchedEffect(exoPlayer) {
        videoTransformationViewModel.setExoPlayer(exoPlayer)
    }

    // Update trimRange from view to viewmodel
    LaunchedEffect(trimRange) {
        videoTransformationViewModel.updateTrimRange(first = trimRange.first, last = trimRange.last)
    }

    // Current position change (By seeking), so we will need to seek to preview thumbnail
    LaunchedEffect(currentPosition) {
        if (currentPosition > 0 && isDraggingTrim) {
            videoTransformationViewModel.exoPlayer?.seekTo(currentPosition)
        }
    }

    // Dragging will pause video
    LaunchedEffect(isDraggingTrim) {
        if (isDraggingTrim) {
            videoTransformationViewModel.setIsPlaying(false)
        }
    }

    // Update correct trim and get thumbnail from selected video
    LaunchedEffect(selectedVideo) {
        videoTransformationViewModel.extractThumbnailsPerSecond(selectedVideo)
        // only call update in case both first and last is 0
        val trimmedRange = videoTransformationViewModel.trimmedRangeSelected.value
        if (trimmedRange.first == 0L && trimmedRange.last == 0L) {
            videoTransformationViewModel.updateTrimRange(
                first = 0L,
                last = selectedVideo.duration.toLong()
            )
        }
    }

    // Base on this flag to start or pause video
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            videoTransformationViewModel.playVideo()
        } else {
            videoTransformationViewModel.pauseVideo()
        }
    }

    // Calculate progress of bottom sheet
    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow { offsetBottomSheet }.collect { offset ->
            if (sheetOffset.floatValue == -1f) {
                sheetOffset.floatValue = offset
            }
            progress.floatValue = (1 - (offset / sheetOffset.floatValue)).format(4).toFloat()
        }
    }


    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Only play in case not show audio bottom sheet
                    if (!isShowAudioBottomSheet) {
                        videoTransformationViewModel.setIsPlaying(true)
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    videoTransformationViewModel.setIsPlaying(false)
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            videoTransformationViewModel.releaseData()
            videoTransformationViewModel.exoPlayer?.release()
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler {
        navController.popBackStack()
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
                        selectedVideo = selectedVideo,
                        isVideoPlaying = isPlaying,
                        startPosition = trimRange.first,
                        currentPlayingPosition = currentPosition,
                        endPosition = trimRange.last,
                        onPlayClick = {
                            videoTransformationViewModel.setIsPlaying(!isPlaying)
                        },
                        onTrimChange = { start, end ->
                            trimRange = LongRange(start, end)
                        },
                        onSeekChange = { position ->
                            videoTransformationViewModel.updateCurrentPosition(position)
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
                            // Open bottom sheet to select audio
                            isShowAudioBottomSheet = true
                            // Pause playing video
                            videoTransformationViewModel.pauseVideo()
                        }

                        TransformationAction.Text -> {
                            isShowTextingOverlay = true
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

    // Bottom sheet audio
    if (isShowAudioBottomSheet) {
        AudioSelectionBottomSheet(
            audioSelectionViewModel = audioSelectionViewModel,
            onSelectedAudio = { selectedAudio ->
                audioSelectionViewModel.onSelectAudio(selectedAudio)
                isShowAudioBottomSheet = false
            },
            onDismissBottomSheet = {
                isShowAudioBottomSheet = false
                // Playing video
                videoTransformationViewModel.playVideo()
            }
        )
    }

    if (isShowTextingOverlay) {
        TextOverlayPreview(onDoneClick = {
            isShowTextingOverlay = false
        })
    }
}

@UnstableApi
@Composable
private fun rememberExoPlayer(
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

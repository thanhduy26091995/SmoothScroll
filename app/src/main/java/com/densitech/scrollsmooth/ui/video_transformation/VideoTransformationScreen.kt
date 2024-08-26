@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.video_transformation

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetCollapsed
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetContent
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetExpanded
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import com.densitech.scrollsmooth.ui.utils.format
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
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
        rememberExoPlayer(context = context, videoUri = Uri.parse(selectedVideo?.videoPath))
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
    var isPlayingPreview by remember {
        mutableStateOf(true)
    }

    val checkPositionHandler = object : Runnable {
        override fun run() {
            if (exoPlayer.currentPosition >= trimRange.last) {
                exoPlayer.pause()
                trimmedHandler.removeCallbacks(this)
            } else {
                trimmedHandler.postDelayed(this, 1000)
            }
        }
    }

    fun playTrimmedVideo(trimRange: LongRange) {
        exoPlayer.seekTo(trimRange.first)

        exoPlayer.play()

        // Use a Handler to check the player's position periodically
        trimmedHandler.post(checkPositionHandler)
    }

    fun pauseTrimmedVideo() {
        trimmedHandler.removeCallbacks(checkPositionHandler)
    }

    LaunchedEffect(selectedVideo) {
        selectedVideo?.let {
            videoTransformationViewModel.extractThumbnailsPerSecond(it)
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
                        onPlayClick = {
                            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                            isPlayingPreview = exoPlayer.playWhenReady

                            if (isPlayingPreview) {
                                playTrimmedVideo(trimRange)
                            } else {
                                pauseTrimmedVideo()
                            }
                        },
                        onTrimChange = { start, end ->
                            trimRange = LongRange(start, end)
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
fun rememberExoPlayer(context: Context, videoUri: Uri): ExoPlayer {
    return remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(videoUri))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }
}

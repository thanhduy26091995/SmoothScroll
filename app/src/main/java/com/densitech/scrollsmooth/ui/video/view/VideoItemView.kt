package com.densitech.scrollsmooth.ui.video.view

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.utils.formatTime
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel.Companion.MAX_DURATION_TIME_TO_SEEK
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoItemView(
    playerPool: PlayerPool,
    isActive: Boolean,
    currentToken: Int,
    currentMediaSource: MediaSource,
    currentRatio: Pair<Int, Int>,
    onReceiveRatio: (Int, Int, Int) -> Unit,
    onPlayerReady: (Int, ExoPlayer?) -> Unit,
    onPlayerDestroy: (Int) -> Unit,
    onPauseClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var ratio by remember { mutableStateOf(currentRatio) }
    var isInView by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0F) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var heightOfSlider by remember { mutableStateOf(2.dp) }
    var onSeekingCurrentDurationPercent by rememberSaveable { mutableFloatStateOf(0F) }
    var currentPreviewOffsetXFrame by remember {
        mutableStateOf(0F)
    }

    val screenWidth1 = with(LocalDensity.current) { 1080.toDp() }
    val itemWidth1 = with(LocalDensity.current) { (1080 / 5).toDp() }

    val videoListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            if (videoSize.width > 0 && videoSize.height > 0) {
                onReceiveRatio.invoke(currentToken, videoSize.width, videoSize.height)
                ratio = Pair(videoSize.width, videoSize.height)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

//            println("CURRENT STATE for $playbackState")
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
//            println("ON RENDER FIRST FRAME FOR $currentToken")
        }
    }

    fun releasePlayer(player: ExoPlayer?) {
        exoPlayer?.removeListener(videoListener)
        playerPool.releasePlayer(currentToken, player ?: exoPlayer)
        exoPlayer = null
    }

    fun setupPlayer(player: ExoPlayer) {
        if (!isInView) {
            releasePlayer(player)
        } else {
            if (player != exoPlayer) {
                releasePlayer(exoPlayer)
            }

            player.run {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                setMediaSource(currentMediaSource)
                seekTo(currentPosition.toLong())
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                exoPlayer = player
                prepare()
                // Notify that player is ready, then add to holder map
                onPlayerReady(currentToken, player)
                addListener(videoListener)
            }
        }
    }

    DisposableEffect(currentToken) {
        isInView = true
        if (exoPlayer == null) {
            playerPool.acquirePlayer(currentToken, ::setupPlayer)
        } else {
            // Notify that player is ready, then add to holder map
            onPlayerReady(currentToken, exoPlayer)
        }

        onDispose {
            isInView = false
            releasePlayer(exoPlayer)
            onPlayerDestroy(currentToken)
        }
    }

    LaunchedEffect(isActive) {
        while (isActive) {
            exoPlayer?.let {
                currentPosition = it.currentPosition / it.duration.toFloat()
                delay(1000)
            }
        }
    }

    exoPlayer?.let { player ->
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                exoPlayer = player,
                ratio = ratio.first / ratio.second.toFloat(),
                onPauseClick = {
                    onPauseClick.invoke(!player.playWhenReady)
                },
                modifier = modifier
            )

            if (!isDraggingSlider) {
//                SeekingTimeView(
//                    currentPosition = onSeekingCurrentDuration,
//                    duration = player.duration,
//                    modifier = Modifier
//                        .padding(bottom = 30.dp)
//                        .align(
//                            Alignment.BottomCenter
//                        )
//                )

                PreviewHolderView(
                    url = "https://firebasestorage.googleapis.com/v0/b/smoothscroll-7252a.appspot.com/o/thumbnails%2F10_20240808181733_7612773-hd_1080_1920_25fps%2Fmedium%2Fthumbnail_0.jpg?alt=media&token=1b81d737-9905-4d1a-b680-173a98c30d4c",
                    percentage = onSeekingCurrentDurationPercent,
                    duration = player.duration,
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                        .align(
                            Alignment.BottomStart
                        ),
                    offsetX = currentPreviewOffsetXFrame
                )
            }

            // 15s
            if (player.duration >= MAX_DURATION_TIME_TO_SEEK) {
                SliderTimeView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightOfSlider)
                        .align(Alignment.BottomCenter),
                    currentValue = currentPosition, onValueChange = {
                        player.seekTo((it * player.duration).toLong())
                    },
                    onDraggedSlider = { isDragged ->
                        isDraggingSlider = isDragged
                        heightOfSlider = if (isDragged) 30.dp else 2.dp
                    },
                    onTempSliderPositionChange = { loadPercent ->
//                        onSeekingCurrentDuration = (loadPercent * player.duration).toLong()
                        onSeekingCurrentDurationPercent = loadPercent

                        currentPreviewOffsetXFrame =
                            ((screenWidth1 - itemWidth1) * onSeekingCurrentDurationPercent).coerceIn(
                                0.dp,
                                (screenWidth1 - itemWidth1)
                            ).value
                        println(currentPreviewOffsetXFrame)
                    })
            }
        }
    }
}

@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    ratio: Float,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validRatio = if (ratio.isNaN() || ratio <= 0) 1f else ratio
    val aspectRatioModifier = if (validRatio > 1) {
        modifier.aspectRatio(validRatio)
    } else {
        modifier.fillMaxSize()
    }

    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = aspectRatioModifier
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                }
            ) {
                exoPlayer.run {
                    playWhenReady = !playWhenReady
                    onPauseClick.invoke()
                }
            }
    )
}



package com.densitech.scrollsmooth.ui.video.view

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool
import kotlinx.coroutines.delay
import java.util.Locale

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
    modifier: Modifier = Modifier
) {
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var ratio by remember { mutableStateOf(currentRatio) }
    var isInView by remember { mutableStateOf(false) }
    var isPause by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0F) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var heightOfSlider by remember { mutableStateOf(2.dp) }
    var onSeekingCurrentDuration by remember { mutableLongStateOf(0) }

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
            println("CURRENT STATE for $playbackState")
        }

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            println("ON RENDER FIRST FRAME FOR $currentToken")
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

    exoPlayer?.let {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                exoPlayer = it,
                ratio = ratio.first / ratio.second.toFloat(),
                onPauseClick = {
                    isPause = !it.playWhenReady
                },
                modifier = modifier
            )

            AnimatedVisibility(visible = isPause, modifier = Modifier.align(Alignment.Center)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_arrow_24),
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.Center)
                        .alpha(0.2f),
                )
            }

            if (isDraggingSlider && exoPlayer != null) {
                SeekingTimeView(
                    currentPosition = onSeekingCurrentDuration,
                    duration = exoPlayer!!.duration,
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                        .align(
                            Alignment.BottomCenter
                        )
                )
            }

            PositionView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightOfSlider)
                    .align(Alignment.BottomCenter),
                currentValue = currentPosition, onValueChange = {
                    exoPlayer?.seekTo((it * exoPlayer!!.duration).toLong())
                },
                onDraggedSlider = { isDragged ->
                    isDraggingSlider = isDragged
                    heightOfSlider = if (isDragged) 30.dp else 2.dp
                },
                onTempSliderPositionChange = { loadPercent ->
                    onSeekingCurrentDuration = (loadPercent * it.duration).toLong()
                })
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
    val aspectRatioModifier = if (ratio > 1) {
        modifier.aspectRatio(ratio)
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

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionView(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    onDraggedSlider: (Boolean) -> Unit,
    onTempSliderPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderPosition = remember(currentValue) { mutableFloatStateOf(currentValue) }
    val tempSliderPosition = remember { mutableFloatStateOf(currentValue) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState()

    val animatedSliderValue by animateFloatAsState(
        targetValue = sliderPosition.floatValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Slider"
    )

    LaunchedEffect(isDragged.value) {
        onDraggedSlider.invoke(isDragged.value)
    }

    Slider(
        modifier = modifier,
        onValueChange = { progress ->
            sliderPosition.floatValue = progress
            tempSliderPosition.floatValue = progress

            onTempSliderPositionChange.invoke(tempSliderPosition.floatValue)
        },
        thumb = {

        },
        value = if (isDragged.value) tempSliderPosition.floatValue else animatedSliderValue,
        onValueChangeFinished = {
            sliderPosition.floatValue = tempSliderPosition.floatValue
            onValueChange(tempSliderPosition.floatValue)
        },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTickColor = Color.Gray
        )
    )
}

@Composable
fun SeekingTimeView(currentPosition: Long, duration: Long, modifier: Modifier = Modifier) {
    val timeFormat = "%02d:%02d"

    fun formatTime(timeInMilliSeconds: Long): String {
        val timeInSeconds = (timeInMilliSeconds / 1000)
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format(Locale.getDefault(), timeFormat, minutes, seconds)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatTime(currentPosition), fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = " / ", fontSize = 16.sp)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = formatTime(duration), fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
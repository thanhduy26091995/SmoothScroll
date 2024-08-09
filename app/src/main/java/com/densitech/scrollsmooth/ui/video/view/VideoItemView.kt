package com.densitech.scrollsmooth.ui.video.view

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool
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

            if (isDraggingSlider) {
                SeekingTimeView(
                    currentPosition = onSeekingCurrentDuration,
                    duration = player.duration,
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                        .align(
                            Alignment.BottomCenter
                        )
                )
            }

            // 30s
            if (player.duration >= 30000) {
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
                        onSeekingCurrentDuration = (loadPercent * player.duration).toLong()
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



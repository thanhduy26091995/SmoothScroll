package com.densitech.scrollsmooth.ui.video.view

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                println("it.currentPosition ${it.currentPosition} - TOTAL ${it.duration}")
                currentPosition = it.currentPosition / it.duration.toFloat()
                println("currentPosition $currentPosition - TOTAL ${exoPlayer!!.duration}")
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

            PositionView(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomCenter),
                currentValue = currentPosition, onValueChange = {
                    exoPlayer?.seekTo((it * exoPlayer!!.duration).toLong())
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

@Composable
@Preview
fun PreviewDuration() {
    PositionView(currentValue = 0.5f, onValueChange = {

    })
}


@Composable
fun PositionView(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedSliderValue by animateFloatAsState(
        targetValue = currentValue,
        animationSpec = tween(durationMillis = 100), label = "Smooth"
    )

    Slider(
        modifier = modifier,
        value = animatedSliderValue,
        onValueChange = {
            onValueChange(it)
        }
    )
}
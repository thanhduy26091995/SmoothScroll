package com.densitech.scrollsmooth.ui.video_creation.view

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo

@OptIn(UnstableApi::class)
@Composable
fun VideoSelectedView(data: DTOLocalVideo, modifier: Modifier = Modifier) {
    var isPaused by remember {
        mutableStateOf(false)
    }

    var currentPlaybackState by remember {
        mutableIntStateOf(Player.STATE_BUFFERING)
    }

    val ratio = data.width / data.height.toFloat()
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(data.videoPath)))
            playWhenReady = !isPaused

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    currentPlaybackState = playbackState
                }
            })
        }
    }

    LaunchedEffect(data) {
        // Release exo player
        exoPlayer.apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(data.videoPath)))
            prepare()
        }
    }

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

    val validRatio = if (ratio.isNaN() || ratio <= 0 || ratio > 1f) 1f else ratio

    Box(modifier = modifier) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .aspectRatio(validRatio)
                .clickableNoRipple {
                    exoPlayer.run {
                        playWhenReady = !playWhenReady
                        isPaused = !isPaused
                    }
                },
        )

        if (currentPlaybackState != Player.STATE_READY) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        AnimatedVisibility(
            visible = isPaused,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_arrow_24),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.Center)
                    .alpha(0.2f)
            )
        }
    }
}
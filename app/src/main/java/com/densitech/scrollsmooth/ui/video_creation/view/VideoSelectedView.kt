package com.densitech.scrollsmooth.ui.video_creation.view

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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

    val ratio = remember {
        data.width / data.height.toFloat()
    }
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(Uri.parse(data.videoPath)))
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                prepare()
                playWhenReady = true
            }
    }

    LaunchedEffect(data) {
        // Release exo player
        exoPlayer.apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(data.videoPath)))
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

    val validRatio = if (ratio.isNaN() || ratio <= 0) 1f else ratio
    val aspectRatioModifier = Modifier.aspectRatio(validRatio)

    Box(modifier = modifier.fillMaxSize()) {
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = aspectRatioModifier
                .align(Alignment.Center)
                .clickableNoRipple {
                    exoPlayer.run {
                        playWhenReady = !playWhenReady
                        isPaused = !isPaused
                    }
                },
        )

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
                    .alpha(0.2f),
            )
        }
    }
}
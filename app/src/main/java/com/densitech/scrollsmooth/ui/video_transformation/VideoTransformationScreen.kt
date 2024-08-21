package com.densitech.scrollsmooth.ui.video_transformation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoTransformationScreen(
    navController: NavController,
    videoCreationViewModel: VideoCreationViewModel,
    videoTransformationViewModel: VideoTransformationViewModel,
) {
    val selectedVideo by videoCreationViewModel.selectedVideo.collectAsState()

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(Uri.parse(selectedVideo?.videoPath)))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoPlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier
                .padding(top = 48.dp, bottom = 50.dp)
                .clip(RoundedCornerShape(24.dp))
        )
    }
}

@Composable
private fun VideoPlayerView(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier
            .fillMaxSize()
    )
}
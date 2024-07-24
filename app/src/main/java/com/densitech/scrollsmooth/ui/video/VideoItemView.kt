package com.densitech.scrollsmooth.ui.video

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoItemView(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)

        onDispose {
            exoPlayer.release()
        }
    }


//    AndroidView(modifier = modifier
//        .fillMaxHeight()
//        .background(Color.Green),
//        factory = {
//            PlayerView(context).apply {
//                player = exoPlayer
//                useController = true
//                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
//            }
//        })

    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "TEST", modifier = Modifier.fillMaxSize())
    }
}


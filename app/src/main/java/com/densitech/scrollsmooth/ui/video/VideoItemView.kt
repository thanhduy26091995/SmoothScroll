package com.densitech.scrollsmooth.ui.video

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.preload.PreloadMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool

@OptIn(UnstableApi::class)
@Composable
fun VideoItemView(
    viewCounter: Int,
    playerPool: PlayerPool,
    currentToken: Int,
    currentMediaSource: PreloadMediaSource,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var exoPlayer: ExoPlayer? = remember {
        null
    }

    var isInView: Boolean = remember {
        false
    }

    var token: Int = remember {
        -1
    }

    var mediaSource: PreloadMediaSource? = remember {
        null
    }

    fun releasePlayer(player: ExoPlayer?) {
        playerPool.releasePlayer(token, player ?: exoPlayer)
        exoPlayer = null
    }

    fun setupPlayer(player: ExoPlayer) {
        if (!isInView) {
            releasePlayer(player)
        } else {
            if (player != exoPlayer) {
                releasePlayer(player)
            }

            player.run {
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                mediaSource?.let {
                    setMediaSource(it)
                }
                seekTo(currentPosition)
                exoPlayer = player
                player.prepare()
            }
        }
    }


    DisposableEffect(true) {
        mediaSource = currentMediaSource
        token = currentToken

        isInView = true
        if (exoPlayer == null) {
            playerPool.acquirePlayer(token, ::setupPlayer)
        }

        Log.d("viewpager", "onViewAttachedToWindow: $viewCounter")

        onDispose {
            isInView = false
            releasePlayer(exoPlayer)
            mediaSource?.preload(0)
            Log.d("viewpager", "onViewDetachedFromWindow: $viewCounter")
        }
    }

    AndroidView(modifier = modifier
        .fillMaxHeight(),
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        })
}


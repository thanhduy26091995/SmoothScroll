package com.densitech.scrollsmooth.ui.video

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
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
    onPlayerReady: (Int, ExoPlayer?) -> Unit,
    onPlayerDestroy: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isInView by remember { mutableStateOf(false) }

    fun releasePlayer(player: ExoPlayer?) {
        playerPool.releasePlayer(currentToken, player ?: exoPlayer)
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
                setMediaSource(currentMediaSource)
                seekTo(currentPosition)
                exoPlayer = player
                player.prepare()
            }
        }
    }

    DisposableEffect(true) {
        isInView = true
        if (exoPlayer == null) {
            playerPool.acquirePlayer(currentToken, ::setupPlayer)
        }

        onPlayerReady.invoke(currentToken, exoPlayer)
        Log.d("viewpager", "onViewAttachedToWindow: $viewCounter")

        onDispose {
            isInView = false
            releasePlayer(exoPlayer)
            currentMediaSource.preload(0)
            Log.d("viewpager", "onViewDetachedFromWindow: $viewCounter")
            onPlayerDestroy.invoke(currentToken)
        }
    }

    AndroidView(modifier = modifier
        .fillMaxHeight()
        .clickable {
            exoPlayer?.run {
                playWhenReady = !playWhenReady
            }
        },
        factory = {
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = {
            it.player = exoPlayer
        })
}


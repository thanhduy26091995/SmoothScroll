package com.densitech.scrollsmooth.ui.video

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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool

@OptIn(UnstableApi::class)
@Composable
fun VideoItemView(
    playerPool: PlayerPool,
    currentToken: Int,
    currentMediaSource: MediaSource,
    onPlayerReady: (Int, ExoPlayer?) -> Unit,
    onPlayerDestroy: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
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
                releasePlayer(exoPlayer)
            }

            player.run {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                setMediaSource(currentMediaSource)
                seekTo(currentPosition)
                exoPlayer = player
                prepare()
                // Notify that player is ready, then add to holder map
                onPlayerReady(currentToken, exoPlayer)
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

    if (exoPlayer != null) {
        PlayerSurface(
            player = exoPlayer!!,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = modifier
                .fillMaxHeight()
                .clickable {
                    exoPlayer?.run { playWhenReady = !playWhenReady }
                })
    }


//    AndroidView(
//        modifier = modifier
//            .fillMaxHeight()
//            .clickable {
//                exoPlayer?.run { playWhenReady = !playWhenReady }
//            },
//        factory = {
//            PlayerView(context).apply {
//                useController = false
//                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
//            }
//        },
//        update = {
//            it.player = exoPlayer
//        }
//    )
}

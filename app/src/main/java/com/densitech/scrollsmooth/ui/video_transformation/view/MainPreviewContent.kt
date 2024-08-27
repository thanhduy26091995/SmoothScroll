package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction

@Composable
fun MainPreviewContent(
    exoPlayer: ExoPlayer,
    currentFraction: Float,
    onBackClick: () -> Unit,
    onActionClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
    ) {
        VideoPlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        )

        AnimatedVisibility(visible = currentFraction == 0f) {

        }
        if (currentFraction == 0f) {
            IconButton(
                onClick = {
                    onBackClick.invoke()
                }, modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            IconButton(
                onClick = {
                    onBackClick.invoke()
                }, modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }

            TransformationActionView(
                onActionClick = onActionClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 10.dp)
            )
        }
    }
}

@Composable
fun VideoPlayerView(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier
            .fillMaxSize()
    )
}


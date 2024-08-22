package com.densitech.scrollsmooth.ui.video_transformation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.densitech.scrollsmooth.ui.main.Screen
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
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

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val (videoContainerView, editVideoView, nextView) = createRefs()

        Box(
            modifier = Modifier
                .constrainAs(videoContainerView) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(editVideoView.top)
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints
                }
                .padding(top = 50.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            VideoPlayerView(
                exoPlayer = exoPlayer,
                modifier = Modifier
                    .fillMaxSize()
            )

            IconButton(
                onClick = {
                    navController.popBackStack()
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
                    navController.navigate(Screen.Home.route)
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
                onActionClick = {

                }, modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 10.dp)
            )
        }

        TextButton(onClick = { /*TODO*/ },
            modifier = Modifier
                .padding(16.dp)
                .constrainAs(editVideoView) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                }
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
        ) {
            Text("Edit Video", fontSize = 12.sp)
        }

        TextButton(onClick = { /*TODO*/ },
            modifier = Modifier
                .padding(16.dp)
                .constrainAs(nextView) {
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Blue),
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
        ) {
            Text("Next")
        }
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

@Composable
private fun TransformationActionView(
    onActionClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions =
        listOf(TransformationAction.Music, TransformationAction.Text, TransformationAction.Sticker)

    Row(modifier = modifier) {
        actions.forEach {
            TransformationActionItemView(action = it, onIconClick = onActionClick)

            Spacer(modifier = Modifier.width(7.dp))
        }
    }
}

@Composable
private fun TransformationActionItemView(
    action: TransformationAction,
    onIconClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.DarkGray)
            .clickableNoRipple {
                onIconClick.invoke(action)
            }
    ) {
        Icon(
            painter = painterResource(id = action.iconId),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center)
        )
    }
}
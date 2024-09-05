package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams
import com.densitech.scrollsmooth.ui.text.view.stringToFont
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction
import kotlin.math.roundToInt

@Composable
fun MainPreviewContent(
    exoPlayer: ExoPlayer,
    currentFraction: Float,
    isShowingTextOverlay: Boolean,
    textOverlayList: List<TextOverlayParams>,
    onTransformGestureChanged: (String, Offset, Float, Float) -> Unit,
    onBackClick: () -> Unit,
    onActionClick: (TransformationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val actions = remember {
        listOf(TransformationAction.Music, TransformationAction.Text, TransformationAction.Sticker)
    }

    var heightOfTopButton by remember {
        mutableStateOf(0.dp)
    }

    var heightOfBottomButtons by remember {
        mutableStateOf(0.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // The video player
        VideoPlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        )

        // Back and more buttons - make sure these are not blocked
        if (currentFraction == 0f && !isShowingTextOverlay) {
            IconButton(
                onClick = {
                    onBackClick.invoke()
                }, modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .onGloballyPositioned {
                        heightOfTopButton = with(density) {
                            (it.size.height + it.size.height.toFloat() / 2)
                                .toDp()
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }

            IconButton(
                onClick = {
                    // Do something
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
                actions = actions,
                onActionClick = onActionClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomStart)
                    .padding(bottom = 10.dp)
                    .onGloballyPositioned {
                        heightOfBottomButtons = with(density) {
                            (it.size.height + it.size.height.toFloat() / 2)
                                .toDp()
                        }
                    }
            )
        }

        // Text overlays with scaling and rotation
        textOverlayList.forEach { overlay ->
            Box(
                modifier = Modifier
                    .padding(top = heightOfTopButton)
                    .padding(bottom = heightOfBottomButtons)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            // Pass the transformation to the callback function
                            onTransformGestureChanged(overlay.key, pan, zoom, rotation)
                        }
                    }
            ) {
                // This is the draggable, scalable, and rotatable text overlay
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                overlay.textX.roundToInt(),
                                overlay.textY.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = overlay.scale
                            scaleY = overlay.scale
                            rotationZ = overlay.rotationAngle
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (overlay.textColor != Color.White) Color.Transparent else Color.Black)
                        .padding(6.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                // Handle tap gestures on the text overlay
                                // Tapping the overlay won't block other UI actions like button clicks
                            }
                        }
                ) {
                    // Text inside the overlay
                    Text(
                        text = overlay.text,
                        fontSize = with(LocalDensity.current) { (overlay.fontSize * overlay.scale).toSp() },
                        color = overlay.textColor,
                        fontFamily = stringToFont(overlay.font),
                    )
                }
            }
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


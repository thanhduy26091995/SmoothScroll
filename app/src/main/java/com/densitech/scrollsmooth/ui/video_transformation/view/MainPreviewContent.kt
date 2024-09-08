package com.densitech.scrollsmooth.ui.video_transformation.view

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_transformation.model.TransformationAction
import kotlin.math.cos
import kotlin.math.sin

@Suppress("DEPRECATION")
@Composable
fun MainPreviewContent(
    exoPlayer: ExoPlayer,
    currentFraction: Float,
    isShowingTextOverlay: Boolean,
    textOverlayList: List<TextOverlayParams>,
    onTransformGestureChanged: (String, Offset, Float, Float) -> Unit,
    onBackClick: () -> Unit,
    onActionClick: (TransformationAction) -> Unit,
    onTextOverlayDeleted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // To control vibrate in case user drag text into delete area
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // State management
    // Keep track height of top button
    var heightOfTopButton by remember { mutableStateOf(0.dp) }
    // Keep track height of button actions
    var heightOfBottomButtons by remember { mutableStateOf(0.dp) }
    // Keep track of drag state of text overlay
    var isDraggingText by remember { mutableStateOf(false) }
    // Keep track state of dragging, if dragged into delete area
    var isOverTarget by remember { mutableStateOf(false) }
    // Target bounds for drag to delete area, default is temp value, will update later
    val targetBounds = remember { mutableStateOf(Rect(0f, 0f, 200f, 200f)) }
    // Keep track of text overlay to delete, when drag text into delete area, it will save the key of overlay
    var textOverlayToRemove by remember { mutableStateOf("") }

    LaunchedEffect(isDraggingText) {
        // Once end drag, need to check to remove text overlay if needed
        if (!isDraggingText) {
            onTextOverlayDeleted.invoke(textOverlayToRemove)
        }
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
        if (currentFraction == 0f && !isShowingTextOverlay && !isDraggingText) {
            TopActionButtons(
                onBackClick = {
                    onBackClick.invoke()
                },
                onMoreClick = {},
                onHeightChange = {
                    heightOfTopButton = it
                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            TransformationActionView(
                actions = listOf(
                    TransformationAction.Music,
                    TransformationAction.Text,
                    TransformationAction.Sticker
                ),
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

        // Show drag to delete
        if (isDraggingText) {
            DragToDeleteView(
                onReceiveTargetBounds = { rect ->
                    targetBounds.value = rect
                }, modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            )
        }

        textOverlayList.forEach { overlay ->
            DraggableTextOverlay(
                overlay = overlay,
                targetBounds = targetBounds.value,
                onTransformGestureChanged = onTransformGestureChanged,
                onTextOverlayToRemove = { key ->
                    textOverlayToRemove = key
                    isOverTarget = true
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                },
                onTextOverlayNoLongerOverTarget = {
                    isOverTarget = false
                    textOverlayToRemove = ""
                },
                isDraggingText = {
                    isDraggingText = it
                },
                modifier = Modifier
                    .padding(16.dp)
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


@Composable
private fun DragToDeleteView(onReceiveTargetBounds: (Rect) -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .onGloballyPositioned {
                val heightOfDeleteArea = with(density) {
                    (it.size.height + it.size.height.toFloat() / 2)
                        .toDp()
                }

                val targetBounds = it
                    .boundsInRoot()
                    .copy(
                        left = it.boundsInRoot().left - it.size.width,
                        right = it.boundsInRoot().right + it.size.width,
                        top = it.boundsInRoot().top - heightOfDeleteArea.value,
                        bottom = it.boundsInRoot().bottom + heightOfDeleteArea.value
                    )
                onReceiveTargetBounds.invoke(targetBounds)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Drag to Delete",
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        )

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .border(
                    width = 1.dp,
                    color = Color.White,
                    shape = CircleShape
                )
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_delete_outline_24),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
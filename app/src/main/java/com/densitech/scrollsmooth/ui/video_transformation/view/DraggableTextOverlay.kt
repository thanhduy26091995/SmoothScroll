package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.text.model.TextOverlayParams
import com.densitech.scrollsmooth.ui.text.view.stringToFont
import com.densitech.scrollsmooth.ui.utils.customDetectTransformGestures
import kotlin.math.roundToInt

@Composable
fun DraggableTextOverlay(
    overlay: TextOverlayParams,
    targetBounds: Rect,
    isDraggingText: (Boolean) -> Unit,
    onTransformGestureChanged: (String, Offset, Float, Float) -> Unit,
    onTextOverlayToRemove: (String) -> Unit,
    onTextOverlayNoLongerOverTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // State management
    var isOverTarget by remember { mutableStateOf(false) }
    var currentTargetBounds by remember {
        mutableStateOf(targetBounds)
    }

    LaunchedEffect(targetBounds) {
        currentTargetBounds = targetBounds
    }

    Box(
        modifier = modifier
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
            .pointerInput(Unit) {
                customDetectTransformGestures(
                    onGestureStart = {
                        isDraggingText.invoke(true)
                    },
                    onGestureEnd = {
                        isDraggingText.invoke(false)
                    }
                ) { _, pan, zoom, rotation ->
                    // Adjust the rotation angle by adding the rotation gesture input
                    overlay.rotationAngle += rotation

                    // Adjust the pan based on current rotation
                    val adjustedPan = adjustPanForRotation(pan, overlay.rotationAngle)

                    // Update the overlay state directly with the adjusted pan
                    overlay.apply {
                        textX += adjustedPan.x
                        textY += adjustedPan.y
                        scale *= zoom
                    }

                    println(currentTargetBounds)
                    // Check if dragged item is within the target bounds
                    val isWithinTarget = overlay.textX in targetBounds.left..targetBounds.right &&
                            overlay.textY in targetBounds.top..targetBounds.bottom

                    if (isWithinTarget && !isOverTarget) {
                        onTextOverlayToRemove(overlay.key)
                        isOverTarget = true
                    } else if (!isWithinTarget && isOverTarget) {
                        onTextOverlayNoLongerOverTarget()
                        isOverTarget = false
                    }

                    // Pass the transformation to the callback function
                    onTransformGestureChanged(
                        overlay.key,
                        adjustedPan,
                        zoom,
                        rotation
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (overlay.textColor != Color.White) Color.Transparent else Color.Black)
                .padding(6.dp)
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

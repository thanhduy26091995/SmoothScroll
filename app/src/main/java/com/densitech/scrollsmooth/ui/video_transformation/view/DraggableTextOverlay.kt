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
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    var currentOverlay by remember {
        mutableStateOf(overlay)
    }
    var isOverTarget by remember { mutableStateOf(false) }
    var currentTargetBounds by remember { mutableStateOf(targetBounds) }
    var currentTextX by remember { mutableFloatStateOf(overlay.textX) }
    var currentTextY by remember { mutableFloatStateOf(overlay.textY) }
    var currentScale by remember { mutableFloatStateOf(overlay.scale) }
    var currentRotation by remember { mutableFloatStateOf(overlay.rotationAngle) }
    var currentAdjustPan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(overlay) {
        currentOverlay = overlay
    }

    LaunchedEffect(targetBounds) {
        currentTargetBounds = targetBounds
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    currentTextX.roundToInt(),
                    currentTextY.roundToInt()
                )
            }
            .graphicsLayer {
                scaleX = currentScale
                scaleY = currentScale
                rotationZ = currentRotation
            }
            .pointerInput(Unit) {
                customDetectTransformGestures(
                    onGestureStart = {
                        isDraggingText.invoke(true)
                    },
                    onGestureEnd = {
                        isDraggingText.invoke(false)
                        // Pass the transformation to the callback function
                        onTransformGestureChanged(
                            overlay.key,
                            Offset(currentTextX, currentTextY),
                            currentScale,
                            currentRotation
                        )
                    }
                ) { _, pan, zoom, rotation ->
                    // Adjust the rotation angle by adding the rotation gesture input
                    currentRotation += rotation

                    // Adjust the pan based on current rotation
                    currentAdjustPan = adjustPanForRotation(pan, currentRotation)

                    // Update the overlay state directly with the adjusted pan
                    currentTextX += currentAdjustPan.x
                    currentTextY += currentAdjustPan.y
                    currentScale *= zoom

                    println("CURRENT SCALE: $currentScale -- ${currentOverlay.scale}")

                    // Check if dragged item is within the target bounds
                    val isWithinTarget =
                        currentTextX in currentTargetBounds.left..currentTargetBounds.right &&
                                currentTextY in currentTargetBounds.top..currentTargetBounds.bottom

                    if (isWithinTarget && !isOverTarget) {
                        onTextOverlayToRemove(overlay.key)
                        isOverTarget = true
                        // In case user drag text inside of delete area, it will auto scale to 0.5f
                        currentScale = 0.5f
                    } else if (!isWithinTarget && isOverTarget) {
                        onTextOverlayNoLongerOverTarget()
                        isOverTarget = false
                        // In case user drag text outside of delete area, it will update scale back
                        currentScale = currentOverlay.scale
                    }
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

private fun adjustPanForRotation(pan: Offset, rotationAngle: Float): Offset {
    val angleInRadians = Math.toRadians(rotationAngle.toDouble())

    val adjustedPanX = (pan.x * cos(angleInRadians) - pan.y * sin(angleInRadians)).toFloat()
    val adjustedPanY = (pan.x * sin(angleInRadians) + pan.y * cos(angleInRadians)).toFloat()

    return Offset(adjustedPanX, adjustedPanY)
}
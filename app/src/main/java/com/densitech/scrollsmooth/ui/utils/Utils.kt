package com.densitech.scrollsmooth.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs

const val HEIGHT_OF_TRIMMING = 56
const val DEFAULT_FRACTION = 0.4f
const val NUMBER_OF_FRAME_ITEM = 20
const val SMALL_FRACTION_TO_IGNORE = 0.01

fun formatTime(timeInMilliSeconds: Long, timeFormat: String = "%02d:%02d"): String {
    val timeInSeconds = (timeInMilliSeconds / 1000)
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60
    return String.format(Locale.getDefault(), timeFormat, minutes, seconds)
}

suspend fun PointerInputScope.customDetectTransformGestures(
    panZoomLock: Boolean = false,
    onGestureStart: (() -> Unit)? = null,
    onGestureEnd: (() -> Unit)? = null,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) {
    val processingEvents = mutableListOf<PointerId>()
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        val input = awaitFirstDown(requireUnconsumed = false)
        processingEvents.add(input.id)

        // Gesture starts when the first pointer touches the screen
        var gestureStarted = false

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    // Check if the motion is large enough to be considered a gesture
                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop

                        // Trigger onGestureStart when we detect the start of a gesture
                        if (!gestureStarted) {
                            onGestureStart?.invoke()
                            gestureStarted = true
                        }
                    }
                }

                // Perform the gesture if past touch slop
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        // When all pointers are released, remove the input and end the gesture
        processingEvents.remove(input.id)
        if (processingEvents.isEmpty()) {
            // Only trigger onGestureEnd after the gesture has ended
            if (gestureStarted) {
                onGestureEnd?.invoke()
                gestureStarted = false
            }
        }
    }
}

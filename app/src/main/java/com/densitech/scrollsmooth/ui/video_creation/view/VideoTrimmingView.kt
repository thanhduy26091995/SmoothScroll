package com.densitech.scrollsmooth.ui.video_creation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VideoTrimmingView(
    videoDuration: Long,
    onTrimChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var startTrim by remember { mutableLongStateOf(0L) }
    var endTrim by remember { mutableLongStateOf(videoDuration) }
    val handleWidth = 20.dp

    // This will allow us to get the width of the timeline
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val timelineWidth = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val handleWidthPx = with(density) { handleWidth.toPx() }

        val startTrimOffsetDp = with(density) {
            ((startTrim / videoDuration.toFloat()) * timelineWidth - handleWidthPx / 2).toDp()
        }
        val endTrimOffsetDp = with(density) {
            ((endTrim / videoDuration.toFloat()) * timelineWidth - handleWidthPx / 2).toDp()
        }

        Box(
            modifier = Modifier
                .offset(x = startTrimOffsetDp + handleWidth)
                .width(endTrimOffsetDp - startTrimOffsetDp - handleWidth)
                .height(56.dp)
                .drawBehind {
                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, 1.dp.toPx())
                    )

                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx())
                    )
                }
        )

        // Start Handle
        Box(
            modifier = Modifier
                .offset(x = startTrimOffsetDp)
                .size(handleWidth, 56.dp)
                .border(
                    1.dp,
                    Color.Yellow,
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                )
                .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .background(Color.Yellow)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newTrim =
                            (startTrim + (delta / timelineWidth) * videoDuration).toLong()
                        startTrim = newTrim.coerceIn(0L, endTrim)
                    },
                    onDragStopped = {
                        onTrimChange(startTrim, endTrim)
                    },
                    onDragStarted = {
                        println("ON DRAG START")
                    }
                )
        )

        // End Handle
        Box(
            modifier = Modifier
                .offset(x = endTrimOffsetDp)
                .size(handleWidth, 56.dp)
                .border(
                    1.dp,
                    Color.Yellow,
                    shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(Color.Yellow)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newTrim =
                            (endTrim + (delta / timelineWidth) * videoDuration).toLong()
                        endTrim = newTrim.coerceIn(startTrim, videoDuration)
                    },
                    onDragStopped = {
                        onTrimChange(startTrim, endTrim)
                    },
                    onDragStarted = {
                        println("ON DRAG START")
                    }
                )
        )
    }
}

@Composable
@Preview
fun VideoTrimmingPreview() {
    VideoTrimmingView(videoDuration = 10000, onTrimChange = { _, _ ->

    })
}
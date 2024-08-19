package com.densitech.scrollsmooth.ui.video_creation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VideoTrimming(
    videoDuration: Long,
    onTrimChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var startTrim by remember { mutableLongStateOf(0L) }
    var endTrim by remember { mutableLongStateOf(videoDuration) }
    val handleWidth = 16.dp
    val sliderWidth =

    // This will allow us to get the width of the timeline
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val timelineWidth = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val handleWidthPx = with(density) { handleWidth.toPx() }
// Calculate positions in pixels, then convert to Dp
        val startTrimOffsetDp = with(density) {
            ((startTrim / videoDuration.toFloat()) * timelineWidth - handleWidthPx / 2).toDp()
        }
        val endTrimOffsetDp = with(density) {
            ((endTrim / videoDuration.toFloat()) * timelineWidth - handleWidthPx / 2).toDp()
        }
        // Timeline background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Gray)
                .padding(horizontal = handleWidth)
        )

        // Start Handle
        Box(
            modifier = Modifier
                .offset(x = startTrimOffsetDp)
                .size(handleWidth, 40.dp)
                .background(Color.Blue)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newTrim = (startTrim + (delta / timelineWidth) * videoDuration).toLong()
                        startTrim = newTrim.coerceIn(0L, endTrim)
                        onTrimChange(startTrim, endTrim)
                    }
                )
        )

        // End Handle
        Box(
            modifier = Modifier
                .offset(x = endTrimOffsetDp)
                .size(handleWidth, 40.dp)
                .background(Color.Red)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newTrim = (endTrim + (delta / timelineWidth) * videoDuration).toLong()
                        endTrim = newTrim.coerceIn(startTrim, videoDuration)
                        onTrimChange(startTrim, endTrim)
                    }
                )
        )
    }
}

@Composable
@Preview
fun VideoTrimmingPReview() {
    VideoTrimming(videoDuration = 10000, onTrimChange = { _, _ ->

    })
}
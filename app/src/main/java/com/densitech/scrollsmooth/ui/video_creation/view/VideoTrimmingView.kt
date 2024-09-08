package com.densitech.scrollsmooth.ui.video_creation.view

import android.app.UiModeManager.MODE_NIGHT_YES
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.utils.HEIGHT_OF_TRIMMING
import com.densitech.scrollsmooth.ui.video_creation.model.VideoTrimmingParams

@Composable
fun VideoTrimmingView(
    params: VideoTrimmingParams,
    onTrimChange: (Long, Long) -> Unit,
    onSeekChange: (Long) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var startTrimPosition by remember { mutableLongStateOf(params.startPosition) }
    var endTrimPosition by remember { mutableLongStateOf(params.endPosition) }
    var centerLinePosition by remember { mutableLongStateOf(params.currentPosition) }
    var isDraggingDrawable by remember { mutableStateOf(false) }
    val handleWidth = 20.dp
    val centerLinePadding = 15.dp

    // When video playing, changing currentPosition will change center line also
    LaunchedEffect(params.currentPosition) {
        if (params.currentPosition > 0) {
            centerLinePosition = params.currentPosition
        }
    }

    // While seeking center line position, should send a current position to parent
    LaunchedEffect(centerLinePosition) {
        onSeekChange.invoke(centerLinePosition)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val timelineWidth = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val handleWidthPx = with(density) { handleWidth.toPx() }
        val centerLinePaddingOffsetPx = with(density) { centerLinePadding.toPx() }

        val startTrimOffsetDp =
            calculateOffsetDp(
                density,
                startTrimPosition,
                params.videoDuration,
                timelineWidth,
                handleWidthPx
            )
        val endTrimOffsetDp =
            calculateOffsetDp(
                density,
                endTrimPosition,
                params.videoDuration,
                timelineWidth,
                handleWidthPx
            )
        val centerLineOffsetDp = calculateCenterLineOffsetDp(
            density = density,
            centerLinePosition = centerLinePosition,
            videoDuration = params.videoDuration,
            timelineWidth = timelineWidth,
            centerLinePadding = centerLinePaddingOffsetPx,
        )

        DrawTimeline(startTrimOffsetDp, endTrimOffsetDp, handleWidth)

        DrawHandle(
            offset = startTrimOffsetDp,
            size = handleWidth,
            height = HEIGHT_OF_TRIMMING.dp,
            isStart = true,
            onDragStateChange = { isDragging ->
                onDragStateChange(isDragging)

                if (!isDragging) {
                    onTrimChange.invoke(startTrimPosition, endTrimPosition)
                }
                isDraggingDrawable = isDragging
            }
        ) { delta ->
            startTrimPosition = updateTrimPosition(
                delta,
                startTrimPosition,
                0L,
                endTrimPosition,
                timelineWidth,
                params.videoDuration
            )
            if (startTrimPosition > centerLinePosition) {
                centerLinePosition = startTrimPosition
            }
        }

        DrawHandle(
            offset = endTrimOffsetDp,
            size = handleWidth,
            height = HEIGHT_OF_TRIMMING.dp,
            isStart = false,
            onDragStateChange = { isDragging ->
                onDragStateChange(isDragging)

                if (!isDragging) {
                    onTrimChange.invoke(startTrimPosition, endTrimPosition)
                }
                isDraggingDrawable = isDragging
            }
        ) { delta ->
            endTrimPosition = updateTrimPosition(
                delta,
                endTrimPosition,
                startTrimPosition,
                params.videoDuration,
                timelineWidth,
                params.videoDuration
            )
            if (endTrimPosition < centerLinePosition) {
                centerLinePosition = endTrimPosition
            }
        }

        if (params.numberOfThumbnailFrame > 0) {
            DrawCenterLine(
                offset = centerLineOffsetDp,
                isDraggingDrawable = isDraggingDrawable,
                animatedDuration = (params.videoDuration / (params.numberOfThumbnailFrame)).toInt(),
                onDragStateChange = { isDragging ->
                    onDragStateChange(isDragging)
                }
            ) { delta ->
                val newTrim = calculateNewCenterLinePosition(
                    delta,
                    centerLinePosition,
                    timelineWidth,
                    params.videoDuration
                )
                centerLinePosition = newTrim.coerceIn(
                    startTrimPosition,
                    endTrimPosition
                )
            }
        }
    }
}

@Composable
private fun DrawTimeline(startTrimOffsetDp: Dp, endTrimOffsetDp: Dp, handleWidth: Dp) {
    Box(
        modifier = Modifier
            .offset(x = startTrimOffsetDp + handleWidth)
            .width(endTrimOffsetDp - startTrimOffsetDp - handleWidth)
            .height(HEIGHT_OF_TRIMMING.dp)
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
}

@Composable
private fun DrawHandle(
    offset: Dp,
    size: Dp,
    height: Dp,
    isStart: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    onDrag: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(x = offset)
            .size(size, height)
            .border(
                1.dp,
                Color.Yellow,
                shape = if (isStart)
                    RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                else
                    RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
            )
            .clip(
                if (isStart)
                    RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                else
                    RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
            )
            .background(Color.Yellow)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    onDrag(delta)
                },
                onDragStopped = {
                    onDragStateChange(false)
                },
                onDragStarted = {
                    onDragStateChange(true)
                }
            )
    )
}


@Composable
private fun DrawCenterLine(
    offset: Dp,
    isDraggingDrawable: Boolean,
    animatedDuration: Int,
    onDragStateChange: (Boolean) -> Unit,
    onDrag: (Float) -> Unit,
) {
    var isDraggingCenterLine by remember { mutableStateOf(false) }

    val animatedOffset by animateDpAsState(
        targetValue = offset, label = "", animationSpec = tween(
            durationMillis = if (isDraggingDrawable || isDraggingCenterLine) 0 else animatedDuration,
            easing = LinearEasing
        )
    )

    Box(
        modifier = Modifier
            .offset(x = animatedOffset)
            .size(4.dp, HEIGHT_OF_TRIMMING.dp)
            .background(Color.Yellow)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    onDrag(delta)
                },
                onDragStopped = {
                    onDragStateChange(false)
                    isDraggingCenterLine = false
                },
                onDragStarted = {
                    onDragStateChange(true)
                    isDraggingCenterLine = true
                }
            )
    )
}

private fun calculateOffsetDp(
    density: Density,
    trim: Long,
    videoDuration: Long,
    timelineWidth: Float,
    handleWidthPx: Float,
): Dp {
    return with(density) {
        ((trim / videoDuration.toFloat()) * timelineWidth - handleWidthPx / 2).toDp()
    }
}

private fun calculateCenterLineOffsetDp(
    density: Density,
    centerLinePosition: Long,
    videoDuration: Long,
    timelineWidth: Float,
    centerLinePadding: Float,
): Dp {
    with(density) {
        val normalizedPosition =
            (centerLinePosition / videoDuration.toFloat()) * timelineWidth

        val centerLineOffset = if (centerLinePosition == 0L) {
            centerLinePadding.toDp()
        } else {
            (normalizedPosition).toDp()
        }
        return centerLineOffset
    }
}

private fun updateTrimPosition(
    delta: Float,
    trim: Long,
    minValue: Long,
    maxValue: Long,
    timelineWidth: Float,
    videoDuration: Long,
): Long {
    return (trim + (delta / timelineWidth) * videoDuration).toLong().coerceIn(minValue, maxValue)
}

private fun calculateNewCenterLinePosition(
    delta: Float,
    centerLinePosition: Long,
    timelineWidth: Float,
    videoDuration: Long,
): Long {
    return (centerLinePosition + (delta / timelineWidth) * videoDuration).toLong()
}

@Composable
@Preview(uiMode = MODE_NIGHT_YES)
fun VideoTrimmingPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoTrimmingView(
            modifier = Modifier.padding(horizontal = 16.dp),
            params = VideoTrimmingParams(
                startPosition = 0,
                endPosition = 10000,
                videoDuration = 10000,
                numberOfThumbnailFrame = 15,
                currentPosition = 4000,
            ),
            onTrimChange = { _, _ ->

            },
            onSeekChange = {

            },
            onDragStateChange = {

            })
    }
}
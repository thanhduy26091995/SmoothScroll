package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.utils.HEIGHT_OF_TRIMMING
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalThumbnail
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo
import com.densitech.scrollsmooth.ui.video_creation.view.VideoTrimmingView

@Composable
fun SheetExpandedContentView(
    thumbnails: List<DTOLocalThumbnail>,
    selectedVideo: DTOLocalVideo,
    isVideoPlaying: Boolean,
    currentPlayingPosition: Long,
    onPlayClick: () -> Unit,
    onSeekChange: (Long) -> Unit,
    onTrimChange: (Long, Long) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        IconButton(
            onClick = {
                onPlayClick.invoke()
            },
            modifier = Modifier
                .padding(top = 16.dp, start = 10.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) {
            Icon(
                painter = if (isVideoPlaying) painterResource(id = R.drawable.ic_pause) else painterResource(
                    id = R.drawable.ic_play
                ),
                contentDescription = null
            )
        }

        BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
            val viewWidth = maxWidth - 24.dp
            val trimmingWidth = viewWidth - (2 * 16).dp
            val itemWidth = trimmingWidth / thumbnails.size

            Row(
                modifier = Modifier
                    .height(HEIGHT_OF_TRIMMING.dp)
                    .width(trimmingWidth)
                    .align(Alignment.Center)
            ) {
                thumbnails.forEach {
                    Image(
                        bitmap = it.thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(itemWidth),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            VideoTrimmingView(
                modifier = Modifier
                    .width(viewWidth),
                videoDuration = selectedVideo.duration.toLong(),
                numberOfThumbnailFrame = thumbnails.size - SMOOTH_REVERSED_THUMBNAIL,
                currentPosition = currentPlayingPosition,
                onTrimChange = { start, end ->
                    onTrimChange(start, end)
                },
                onSeekChange = onSeekChange,
                onDragStateChange = onDragStateChange
            )
        }
    }
}

private const val SMOOTH_REVERSED_THUMBNAIL = 3
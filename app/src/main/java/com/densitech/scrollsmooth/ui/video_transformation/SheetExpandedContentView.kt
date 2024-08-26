package com.densitech.scrollsmooth.ui.video_transformation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalThumbnail
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo
import com.densitech.scrollsmooth.ui.video_creation.view.VideoTrimmingView

@Composable
fun SheetExpandedContentView(
    thumbnails: List<DTOLocalThumbnail>,
    selectedVideo: DTOLocalVideo,
    isVideoPlaying: Boolean,
    onPlayClick: () -> Unit,
    onTrimChange: (Long, Long) -> Unit,
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
                imageVector = if (isVideoPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                contentDescription = null
            )
        }
        BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
            val viewWidth = maxWidth - 24.dp
            val trimmingWidth = viewWidth - (2 * 16).dp
            val itemWidth = trimmingWidth / thumbnails.size

            Row(
                modifier = Modifier
                    .height(56.dp)
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
                onTrimChange = { start, end ->
                    onTrimChange(start, end)
                })
        }
    }
}
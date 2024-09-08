package com.densitech.scrollsmooth.ui.audio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.utils.formatTime
import com.densitech.scrollsmooth.ui.video_transformation.model.AudioResponse

@Composable
fun AudioItemView(
    audioResponse: AudioResponse,
    isAudioPlaying: Boolean,
    onActionClick: (AudioResponse) -> Unit,
    onSelectAudio: (AudioResponse) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickableNoRipple {
                onSelectAudio.invoke(audioResponse)
            }, verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = audioResponse.thumbnailUrl,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
            )

            IconButton(
                onClick = {
                    onActionClick.invoke(audioResponse)
                }, modifier = Modifier.align(
                    Alignment.Center
                )
            ) {
                Icon(
                    painter = if (isAudioPlaying) painterResource(id = R.drawable.ic_pause) else painterResource(
                        id = R.drawable.ic_play
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp),
                    tint = Color.White
                )
            }
        }

        Column {
            Text(
                audioResponse.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${audioResponse.artist} - ${formatTime(audioResponse.duration * 1000L)}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
@Preview
private fun AudioItemViewPreview() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(10) {
            AudioItemView(
                audioResponse = AudioResponse(
                    audioUrl = "https://storage.googleapis.com/smoothscroll-7252a.appspot.com/audios/Adele%20-%20Hello%20%28Official%20Music%20Video%29.mp3",
                    name = "Adele - Hello (Official Music Video)dsadsadsadsada",
                    artist = "Adeledsaoidjaskl;djsakl;djasd;lsajdl;sajdl;sadks;aldkl;sa",
                    duration = 350,
                    thumbnailUrl = "https://i.ytimg.com/vi/YQHsXMglC9A/maxresdefault.jpg",
                    source = ""
                ),
                onActionClick = { /*TODO*/ },
                onSelectAudio = {},
                isAudioPlaying = false
            )
        }
    }
}
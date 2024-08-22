package com.densitech.scrollsmooth.ui.video_creation.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.utils.formatTime
import com.densitech.scrollsmooth.ui.video_creation.model.DTOLocalVideo

@Composable
fun VideoCreationItemView(
    data: DTOLocalVideo,
    isSelected: Boolean,
    thumbnail: Bitmap?,
    onVideoClick: (DTOLocalVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        Color.White.copy(alpha = 0.8f)
    } else {
        Color.Transparent
    }

    Box(modifier = modifier
        .clickableNoRipple {
            onVideoClick.invoke(data)
        }) {
        if (thumbnail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.8f))
            )
        } else {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = formatTime(data.duration.toLong()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 5.dp, end = 5.dp),
            fontSize = 12.sp
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        )
    }
}
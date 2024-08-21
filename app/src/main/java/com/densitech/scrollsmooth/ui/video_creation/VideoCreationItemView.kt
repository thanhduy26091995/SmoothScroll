package com.densitech.scrollsmooth.ui.video_creation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.utils.formatTime

@Composable
fun VideoCreationItemView(
    data: DTOLocalVideo,
    thumbnail: Bitmap?,
    onVideoClick: (DTOLocalVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.clickableNoRipple {
        onVideoClick.invoke(data)
    }) {
        Image(
            bitmap = thumbnail!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Text(
            text = formatTime(data.duration.toLong()),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 5.dp, end = 5.dp),
            fontSize = 12.sp
        )
    }
}
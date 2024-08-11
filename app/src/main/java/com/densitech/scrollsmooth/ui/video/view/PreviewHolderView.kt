package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.densitech.scrollsmooth.ui.utils.formatTime

@Composable
fun PreviewHolderView(
    url: String,
    duration: Long,
    offsetX: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .offset(x = offsetX.dp)
    ) {
        val screenWidth = maxWidth
        val itemWidth = screenWidth / 5
        val itemHeight = itemWidth * 2

        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(itemHeight)
                .clip(RoundedCornerShape(4.dp))
                .border((0.5).dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .diskCacheKey(url)
                    .memoryCacheKey(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )

            Text(
                text = formatTime(duration),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
@Preview
fun PreviewHolderPreview() {

}
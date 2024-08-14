package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
fun VideoActionView(
    token: Int,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int,
    isDownloaded: Boolean,
    onLikeClick: (Int) -> Unit,
    onCommentClick: (Int) -> Unit,
    onShareClick: (Int) -> Unit,
    onDownloadClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        VideoActionItemView(
            icon = painterResource(id = R.drawable.ic_heart_icon),
            value = "$likeCount",
            onItemClick = {
                onLikeClick.invoke(token)
            }
        )

        VideoActionItemView(
            icon = painterResource(id = R.drawable.ic_comment_icon),
            value = "$commentCount",
            onItemClick = {
                onCommentClick.invoke(token)
            },
            modifier = Modifier.padding(top = 10.dp)
        )

        VideoActionItemView(
            icon = painterResource(id = R.drawable.ic_outline_share_24),
            value = "$shareCount",
            onItemClick = {
                onShareClick.invoke(token)
            },
            modifier = Modifier.padding(top = 10.dp),
        )

        if (!isDownloaded) {
            VideoActionItemView(
                icon = painterResource(id = R.drawable.ic_download_for_offline),
                value = "",
                onItemClick = {
                    onDownloadClick(token)
                },
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun VideoActionItemView(
    icon: Painter,
    value: String,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickableNoRipple {
            onItemClick.invoke()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.White
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
@Preview
private fun VideoActionViewPreview() {
    VideoActionView(
        token = 1,
        likeCount = 10,
        commentCount = 10,
        shareCount = 10,
        isDownloaded = false, {

        },
        {

        },
        {

        },
        {

        })
}
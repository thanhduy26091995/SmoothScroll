package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.densitech.scrollsmooth.R

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight)
                .background(Color.Black)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(screenWidth / 2)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
@Preview
fun LoadingScreenPreview() {
    LoadingScreen()
}
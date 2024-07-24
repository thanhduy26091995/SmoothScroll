@file:OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VideoScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {

    val playerList = videoScreenViewModel.playList.collectAsState()
    val listState = rememberLazyListState()

    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val screenHeight = maxHeight
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                items(playerList.value) {
                    VideoItemView(
                        url = it.url, modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                    )
                }
            }
        }

    }
}

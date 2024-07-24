package com.densitech.scrollsmooth.ui.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VideoScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {

    val playerList = videoScreenViewModel.playList.collectAsState()

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()

        ) {
            items(playerList.value) {
                VideoItemView(
                    url = it.url, modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}
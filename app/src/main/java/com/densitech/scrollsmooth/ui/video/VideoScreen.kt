@file:OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {

    val playerList = videoScreenViewModel.playList.collectAsState()
    val pagerState = rememberPagerState(
        pageCount = {
            playerList.value.size * 400
        },
        initialPage = (playerList.value.size * 400) / 2
    )

    val fling = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(10)
    )

    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val screenHeight = maxHeight

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondBoundsPageCount = 1,
                flingBehavior = fling
            ) { page ->
                val realPage = page % playerList.value.size
                println("PAGE: ${page}, realPage: ${realPage}")
                VideoItemView(
                    url = playerList.value[realPage].url,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight)
                )
            }
        }
    }
}

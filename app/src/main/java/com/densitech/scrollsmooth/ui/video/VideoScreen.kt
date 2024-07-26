@file:OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.video

import android.os.HandlerThread
import android.os.Process
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
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.densitech.scrollsmooth.ui.video.preFetch.MediaItemSource
import com.densitech.scrollsmooth.ui.video.preFetch.MediaSourceManager
import com.densitech.scrollsmooth.ui.video.preFetch.PlayerPool

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {

    val context = LocalContext.current

    var mediaItemDatabase: MediaItemSource? = remember {
        null
    }

    val playbackThread: HandlerThread = remember {
        HandlerThread("playback-thread", Process.THREAD_PRIORITY_AUDIO)
    }

    val loadControl: DefaultLoadControl = remember {
        DefaultLoadControl()
    }

    val renderersFactory = remember {
        DefaultRenderersFactory(context)
    }

    val playerPool: PlayerPool = remember {
        PlayerPool(
            10,
            context,
            playbackThread.looper,
            loadControl,
            renderersFactory,
            DefaultBandwidthMeter.getSingletonInstance(context)
        )
    }

    val mediaSourceManager: MediaSourceManager = remember {
        MediaSourceManager(
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context)),
            playbackThread.looper,
            loadControl.allocator,
            renderersFactory,
            DefaultTrackSelector(context),
            DefaultBandwidthMeter.getSingletonInstance(context)
        )
    }

    var viewCounter = remember {
        0
    }

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

    LaunchedEffect(playerList.value) {
        if (playerList.value.isNotEmpty()) {
            mediaItemDatabase = MediaItemSource(mediaItems = playerList.value)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            pagerState.currentPage
        }.collect { page ->
            val mediaItemHorizon = page + mediaItemDatabase!!.rCacheSize
            val reachableMediaItems = mediaItemDatabase?.get(page + 1, toIndex = mediaItemHorizon)
            if (reachableMediaItems != null) {
                mediaSourceManager.addAlls(reachableMediaItems)
            }
        }
    }

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
                val mediaList = mediaItemDatabase?.mediaItems ?: return@VerticalPager

                val realPage = page % mediaList.count()
                val mediaItem = mediaList[realPage]
                val mediaSource = mediaSourceManager[mediaItem]

                VideoItemView(
                    viewCounter = viewCounter++,
                    playerPool = playerPool,
                    currentToken = realPage,
                    currentMediaSource = mediaSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight)
                )
            }
        }
    }
}

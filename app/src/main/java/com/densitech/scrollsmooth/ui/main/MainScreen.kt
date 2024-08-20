@file:kotlin.OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.main

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.densitech.scrollsmooth.ui.video.view.VideoScreen
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.densitech.scrollsmooth.ui.video_creation.VideoCreationScreen
import com.densitech.scrollsmooth.ui.video_creation.VideoCreationViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    videoScreenViewModel: VideoScreenViewModel = hiltViewModel(),
    videoCreationViewModel: VideoCreationViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(TabItem.HOME, TabItem.SEARCH, TabItem.ADD, TabItem.PROFILE)

    val homeVideoPagerState = rememberPagerState(
        pageCount = {
            1000
        },
        initialPage = 0
    )


    val permissionArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val mediaPermissions = rememberMultiplePermissionsState(
        permissions = permissionArray,
        onPermissionsResult = {

        })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Black.copy(alpha = 0.1f),
                indicator = {

                }
            ) {
                tabTitles.forEachIndexed { index, tabItem ->
                    TabItemView(
                        tabItem = tabItem,
                        index = index,
                        isSelected = selectedTabIndex == index
                    ) {
                        selectedTabIndex = it
                    }
                }
            }
        }) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (selectedTabIndex) {
                0 -> VideoScreen(homeVideoPagerState, videoScreenViewModel)
                1 -> TabContent("Coming soon")
                2 -> {
                    // Check permission first
                    if (mediaPermissions.allPermissionsGranted) {
                        VideoCreationScreen(viewModel = videoCreationViewModel)
                    } else {
                        mediaPermissions.launchMultiplePermissionRequest()
                    }
                }

                3 -> TabContent("Coming soon")
            }
        }
    }
}

@Composable
fun TabItemView(tabItem: TabItem, index: Int, isSelected: Boolean, onItemClick: (Int) -> Unit) {
    Tab(
        modifier = Modifier
            .padding(bottom = 32.dp, top = 8.dp),
        selected = isSelected,
        interactionSource = object : MutableInteractionSource {
            override val interactions: Flow<Interaction> = emptyFlow()

            override suspend fun emit(interaction: Interaction) {}

            override fun tryEmit(interaction: Interaction) = true
        },
        onClick = {
            onItemClick.invoke(index)
        }) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = tabItem.icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }
    }
}

@Composable
fun TabContent(content: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
        ) {
            Text(
                text = content,
                fontSize = 24.sp,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.Center)
            )
        }
    }
}
package com.densitech.scrollsmooth.ui.main

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(videoScreenViewModel: VideoScreenViewModel = hiltViewModel()) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(TabItem.HOME, TabItem.SEARCH, TabItem.ADD, TabItem.PROFILE)

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
                0 -> VideoScreen(videoScreenViewModel)
                1 -> TabContent("Content for Tab 2")
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
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
    ) {
        Text(
            text = content,
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}
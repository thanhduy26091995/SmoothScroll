package com.densitech.scrollsmooth.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.video.view.VideoScreen


@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Trang chủ", "Home")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        modifier = Modifier
                            .background(Color.Red)
                            .padding(vertical = 8.dp),
                        selected = selectedTabIndex == -index,
                        onClick = {
                            selectedTabIndex = index
                        }) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_play_arrow_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )

                            Text(text = title, fontSize = 13.sp)
                        }
                    }
                }
            }
        }) { _ ->
        when (selectedTabIndex) {
            0 -> VideoScreen()
            1 -> TabContent("Content for Tab 2")
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
@file:kotlin.OptIn(ExperimentalFoundationApi::class)

package com.densitech.scrollsmooth.ui.main

import android.os.Build
import androidx.annotation.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.densitech.scrollsmooth.ui.video.view.VideoScreen
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel
import com.densitech.scrollsmooth.ui.video_creation.view.VideoCreationScreen
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import com.densitech.scrollsmooth.ui.video_transformation.view.VideoTransformationScreen
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.VideoTransformationViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    videoScreenViewModel: VideoScreenViewModel = hiltViewModel(),
    videoCreationViewModel: VideoCreationViewModel = hiltViewModel(),
    videoTransformationViewModel: VideoTransformationViewModel = hiltViewModel(),
) {
    val tabTitles = listOf(Screen.Home, Screen.Search, Screen.Add, Screen.Profile)

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination?.route == Screen.Add.route || currentDestination?.route == Screen.VideoTransformation.route) {
                return@Scaffold
            }

            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.1f),
                modifier = Modifier.height(80.dp)
            ) {
                tabTitles.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }

                                // Avoid multiple copies of the same destination when
                                // Re-selecting the same item
                                launchSingleTop = true
                                // Restore state when Re-selecting a previously selected item
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.resourceId),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                        })
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Home.route) {
                VideoScreen(
                    pagerState = homeVideoPagerState,
                    videoScreenViewModel = videoScreenViewModel
                )
            }

            composable(Screen.Search.route) {
                TabContent("Coming soon")
            }

            composable(Screen.Add.route) {
                if (mediaPermissions.allPermissionsGranted) {
                    VideoCreationScreen(
                        navController = navController,
                        viewModel = videoCreationViewModel
                    )
                } else {
                    mediaPermissions.launchMultiplePermissionRequest()
                }
            }

            composable(Screen.Profile.route) {
                TabContent("Coming soon")
            }

            composable(Screen.VideoTransformation.route) {
                VideoTransformationScreen(
                    navController = navController,
                    videoCreationViewModel = videoCreationViewModel,
                    videoTransformationViewModel = videoTransformationViewModel,
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
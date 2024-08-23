@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.video_transformation

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetCollapsed
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetContent
import com.densitech.scrollsmooth.ui.bottom_sheet.SheetExpanded
import com.densitech.scrollsmooth.ui.utils.format
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoTransformationScreen(
    navController: NavController,
    videoCreationViewModel: VideoCreationViewModel,
    videoTransformationViewModel: VideoTransformationViewModel,
) {
    val selectedVideo by videoCreationViewModel.selectedVideo.collectAsState()

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(Uri.parse(selectedVideo?.videoPath)))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }

    val lifeCycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    exoPlayer.play()
                }

                Lifecycle.Event.ON_STOP -> {
                    exoPlayer.stop()
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)

        onDispose {
            exoPlayer.release()
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val localConfiguration = LocalConfiguration.current
    val playerHeight = localConfiguration.screenHeightDp.dp - 72.dp

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        ),

        )
    val offsetBottomSheet by remember(scaffoldState) {
        derivedStateOf {
            runCatching {
                scaffoldState.bottomSheetState.requireOffset()
            }.getOrDefault(0f)
        }
    }

    val bottomSheetRadius = 0.dp

    val sheetToggle: () -> Unit = {
        scope.launch {
            if (!scaffoldState.bottomSheetState.hasExpandedState) {
                scaffoldState.bottomSheetState.expand()
            } else {
                scaffoldState.bottomSheetState.hide()
            }
        }
    }

    val progress = remember { mutableFloatStateOf(0f) }
    val sheetOffset = remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow { offsetBottomSheet }.collect { offset ->
            if (sheetOffset.floatValue == -1f) {
                sheetOffset.floatValue = offset
            }
            progress.floatValue = (1 - (offset / sheetOffset.floatValue)).format(5).toFloat()
        }
    }

    BottomSheetScaffold(
        sheetDragHandle = {

        },
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(topStart = bottomSheetRadius, topEnd = bottomSheetRadius),
        sheetContent = {
            SheetContent {
                SheetExpanded {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                SheetCollapsed(
                    isCollapsed = !scaffoldState.bottomSheetState.isVisible,
                    currentFraction = progress.floatValue,
                    onSheetClick = sheetToggle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { /*TODO*/ },
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.DarkGray),
                            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
                        ) {
                            Text("Edit Video", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { /*TODO*/ },
                            modifier = Modifier
                                .padding(16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Blue),
                            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        },
        sheetPeekHeight = 72.dp
    ) { innerPadding ->
        MainContent(
            exoPlayer = exoPlayer,
            onBackClick = {
                navController.popBackStack()
            },
            modifier = Modifier
                .padding(top = 50.dp, bottom = 20.dp)
                .padding(innerPadding)
                .fillMaxWidth()
                .height(playerHeight * (1 - progress.floatValue))
                .clip(RoundedCornerShape(24.dp)),
        )
    }
}

@Composable
private fun VideoPlayerView(exoPlayer: ExoPlayer, modifier: Modifier = Modifier) {
    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier
            .fillMaxSize()
    )
}

@Composable
private fun MainContent(
    exoPlayer: ExoPlayer,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
    ) {
        VideoPlayerView(
            exoPlayer = exoPlayer,
            modifier = Modifier
                .fillMaxSize()
        )

        IconButton(
            onClick = {
                onBackClick.invoke()
            }, modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .align(Alignment.TopStart)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }

        IconButton(
            onClick = {
                onBackClick.invoke()
            }, modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Color.DarkGray)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More"
            )
        }

        TransformationActionView(
            onActionClick = {

            }, modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp)
        )
    }
}
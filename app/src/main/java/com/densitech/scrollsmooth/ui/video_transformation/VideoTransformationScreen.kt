@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.video_transformation

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import com.densitech.scrollsmooth.ui.utils.format
import com.densitech.scrollsmooth.ui.video_creation.viewmodel.VideoCreationViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoTransformationScreen(
    navController: NavController,
    videoCreationViewModel: VideoCreationViewModel,
    videoTransformationViewModel: VideoTransformationViewModel,
) {
    val thumbnails by videoTransformationViewModel.thumbnails.collectAsState()
    val selectedVideo by videoCreationViewModel.selectedVideo.collectAsState()
    val context = LocalContext.current
    val exoPlayer =
        rememberExoPlayer(context = context, videoUri = Uri.parse(selectedVideo?.videoPath))

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
    val playerHeight = localConfiguration.screenHeightDp.dp - 72.dp - 50.dp
    val deviceWidth = localConfiguration.screenWidthDp.dp

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
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

    val progress = rememberSaveable { mutableFloatStateOf(0f) }
    val sheetOffset = rememberSaveable { mutableFloatStateOf(-1f) }

    LaunchedEffect(selectedVideo) {
        selectedVideo?.let {
            videoTransformationViewModel.extractThumbnailsPerSecond(it)
        }
    }

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
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .height(40.dp)
                                .align(Alignment.Center)
                        ) {
                            items(thumbnails) {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(30.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
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
                            onClick = {
                                scope.launch {
                                    scaffoldState.bottomSheetState.expand()
                                }
                            },
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
        Box(
            modifier = Modifier
                .padding(top = 50.dp)
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (progress.floatValue > 0) {
                IconButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    }, modifier = Modifier
                        .padding(top = 16.dp, start = 10.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand"
                    )
                }
            }

            MainPreviewContent(
                exoPlayer = exoPlayer,
                currentFraction = progress.floatValue,
                onBackClick = {
                    navController.popBackStack()
                },
                modifier = Modifier
                    .width(deviceWidth * (1 - progress.floatValue))
                    .height(playerHeight * (1 - progress.floatValue))
                    .clip(RoundedCornerShape(24.dp))
                    .align(Alignment.TopCenter)
                    .then(
                        if (progress.floatValue > 0f) {
                            val alpha = (progress.floatValue + DEFAULT_FRACTION).coerceIn(0f, 1f)
                            Modifier.border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = alpha),
                                shape = RoundedCornerShape(24.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

@UnstableApi
@Composable
fun rememberExoPlayer(context: Context, videoUri: Uri): ExoPlayer {
    return remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(videoUri))
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true
        }
    }
}

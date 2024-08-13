package com.densitech.scrollsmooth.ui.video.view

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
import com.densitech.scrollsmooth.ui.video.PlayerSurface
import com.densitech.scrollsmooth.ui.video.SURFACE_TYPE_SURFACE_VIEW
import com.densitech.scrollsmooth.ui.video.model.MediaInfo
import com.densitech.scrollsmooth.ui.video.model.MediaThumbnailDetail
import com.densitech.scrollsmooth.ui.video.prefetch.PlayerPool
import com.densitech.scrollsmooth.ui.video.viewmodel.VideoScreenViewModel.Companion.MAX_DURATION_TIME_TO_SEEK
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@OptIn(UnstableApi::class)
@Composable
fun VideoItemView(
    playerPool: PlayerPool,
    isActive: Boolean,
    currentToken: Int,
    currentMediaSource: MediaSource,
    mediaInfo: MediaInfo,
    onReceiveRatio: (Int, Int, Int) -> Unit,
    onPlayerReady: (Int, ExoPlayer?) -> Unit,
    onPlayerDestroy: (Int) -> Unit,
    onPauseClick: (Boolean) -> Unit,
    onDownloadVideoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var ratio by remember {
        mutableStateOf(
            Pair(
                mediaInfo.metadata.width.toInt(),
                mediaInfo.metadata.height.toInt()
            )
        )
    }
    var isInView by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0F) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var heightOfSlider by remember { mutableStateOf(SliderHeightDefault) }
    var onSeekingCurrentDuration by rememberSaveable { mutableLongStateOf(0) }
    var onSeekingCurrentDurationPercent by remember { mutableFloatStateOf(0F) }
    var currentPreviewOffsetXFrame by remember { mutableFloatStateOf(0F) }

    val nearestThumbnail by remember(onSeekingCurrentDuration, mediaInfo.thumbnails.medium) {
        derivedStateOf {
            mediaInfo.thumbnails.medium.findLast { it.time * 1000 <= onSeekingCurrentDuration }
        }
    }

    val currentDensity = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val itemWidthDp = screenWidthDp / ItemCount

    // Function to update preview offset
    fun updatePreviewOffset(
        onSeekingCurrentDurationPercent: Float,
        screenWidthDp: Int,
        itemWidthDp: Int,
    ): Float {
        val screenWidthPx = with(currentDensity) { screenWidthDp.dp.toPx() }
        val itemWidthPx = with(currentDensity) { itemWidthDp.dp.toPx() }
        val offsetPx = ((screenWidthPx - itemWidthPx) * onSeekingCurrentDurationPercent)
            .coerceIn(0f, (screenWidthPx - itemWidthPx))
        return with(currentDensity) { offsetPx.toDp().value }
    }

    val updateSeekingCurrentDurationPercent by rememberUpdatedState(onSeekingCurrentDurationPercent)
    val updateScreenWidthDp by rememberUpdatedState(screenWidthDp)
    val updateItemWidthDp by rememberUpdatedState(itemWidthDp)

    // LaunchedEffect to update preview offset
    LaunchedEffect(onSeekingCurrentDuration) {
        currentPreviewOffsetXFrame = updatePreviewOffset(
            onSeekingCurrentDurationPercent = updateSeekingCurrentDurationPercent,
            screenWidthDp = updateScreenWidthDp,
            itemWidthDp = updateItemWidthDp
        )
    }

    // LaunchedEffect to handle player position updates
    LaunchedEffect(isActive) {
        while (isActive) {
            exoPlayer?.let {
                currentPosition = it.currentPosition / it.duration.toFloat()
                delay(1000)
            }
        }
    }

    val videoListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                onReceiveRatio.invoke(currentToken, videoSize.width, videoSize.height)
                ratio = Pair(videoSize.width, videoSize.height)
            }
        }
    }

    fun releasePlayer(player: ExoPlayer?) {
        exoPlayer?.removeListener(videoListener)
        playerPool.releasePlayer(currentToken, player ?: exoPlayer)
        exoPlayer = null
    }

    fun setupPlayer(player: ExoPlayer) {
        if (!isInView) {
            releasePlayer(player)
        } else {
            if (player != exoPlayer) {
                releasePlayer(exoPlayer)
            }
            player.run {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                setMediaSource(currentMediaSource)
                seekTo(currentPosition.toLong())
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                exoPlayer = player
                prepare()
                onPlayerReady(currentToken, player)
                addListener(videoListener)
            }
        }
    }

    DisposableEffect(currentToken) {
        isInView = true
        if (exoPlayer == null) {
            playerPool.acquirePlayer(currentToken, ::setupPlayer)
        } else {
            onPlayerReady(currentToken, exoPlayer)
        }
        onDispose {
            isInView = false
            releasePlayer(exoPlayer)
            onPlayerDestroy(currentToken)
        }
    }

    exoPlayer?.let { player ->
        ConstraintLayout(
            modifier = modifier.fillMaxSize()
        ) {
            val (videoView, thumbnailView, sliderView, actionView, contentView) = createRefs()

            VideoPlayer(
                exoPlayer = player,
                ratio = ratio.first / ratio.second.toFloat(),
                onPauseClick = {
                    onPauseClick.invoke(!player.playWhenReady)
                },
                modifier = modifier.constrainAs(videoView) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
            )

            if (isDraggingSlider) {
                ThumbnailPreviewView(
                    nearestThumbnail = nearestThumbnail,
                    currentPreviewOffsetXFrame = currentPreviewOffsetXFrame,
                    duration = onSeekingCurrentDuration,
                    modifier = Modifier
                        .padding(bottom = ThumbnailPadding)
                        .constrainAs(thumbnailView) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(sliderView.top)
                            width = Dimension.fillToConstraints
                        }
                )
            }

            if (player.duration >= MAX_DURATION_TIME_TO_SEEK) {
                SliderTimeView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightOfSlider)
                        .constrainAs(sliderView) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                        },
                    currentValue = currentPosition,
                    onValueChange = {
                        player.seekTo((it * player.duration).toLong())
                    },
                    onDraggedSlider = { isDragged ->
                        isDraggingSlider = isDragged
                        heightOfSlider = if (isDragged) SliderHeightDragged else SliderHeightDefault
                    },
                    onTempSliderPositionChange = { loadPercent ->
                        onSeekingCurrentDuration =
                            ((loadPercent * player.duration).toLong() / 500) * 500
                        onSeekingCurrentDurationPercent = loadPercent
                    }
                )
            }

            if (!isDraggingSlider) {
                VideoActionView(
                    token = currentToken,
                    likeCount = 10,
                    commentCount = 10,
                    shareCount = 10,
                    onLikeClick = {},
                    onCommentClick = {},
                    onShareClick = {},
                    onDownloadClick = {
                        onDownloadVideoClick.invoke(currentToken)
                    },
                    modifier = Modifier
                        .constrainAs(actionView) {
                            end.linkTo(parent.end, 16.dp)
                            bottom.linkTo(parent.bottom, 30.dp)
                        }
                )

                OwnerSectionView(
                    owner = mediaInfo.owner.name,
                    content = mediaInfo.title,
                    tags = mediaInfo.tags,
                    onOwnerClick = { owner ->
                        // Handle navigate to owner account here
                        println(owner)
                    },
                    onTagClick = { tag ->
                        // Handle navigate to tag search here
                        println(tag)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .constrainAs(contentView) {
                            start.linkTo(parent.start)
                            end.linkTo(actionView.start)
                            bottom.linkTo(parent.bottom)
                            width = Dimension.fillToConstraints
                        }
                )
            }
        }
    }
}

@Composable
fun ThumbnailPreviewView(
    nearestThumbnail: MediaThumbnailDetail?,
    duration: Long,
    currentPreviewOffsetXFrame: Float,
    modifier: Modifier = Modifier,
) {
    if (nearestThumbnail != null) {
        PreviewHolderView(
            url = nearestThumbnail.thumbnailUrl,
            duration = duration,
            modifier = modifier,
            offsetX = currentPreviewOffsetXFrame
        )
    }
}

@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    ratio: Float,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validRatio = if (ratio.isNaN() || ratio <= 0) 1f else ratio
    val aspectRatioModifier = if (validRatio > 1) {
        modifier.aspectRatio(validRatio)
    } else {
        modifier.fillMaxSize()
    }

    PlayerSurface(
        player = exoPlayer,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = aspectRatioModifier
            .clickableNoRipple {
                exoPlayer.run {
                    playWhenReady = !playWhenReady
                    onPauseClick.invoke()
                }
            }
    )
}

// Define constants for default values
private val SliderHeightDefault = 2.dp
private val SliderHeightDragged = 30.dp
private const val ItemCount = 5
private val ThumbnailPadding = 10.dp

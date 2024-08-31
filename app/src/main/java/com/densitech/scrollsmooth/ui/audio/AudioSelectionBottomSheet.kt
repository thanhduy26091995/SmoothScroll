package com.densitech.scrollsmooth.ui.audio

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.densitech.scrollsmooth.ui.video_transformation.model.AudioResponse

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSelectionBottomSheet(
    audioSelectionViewModel: AudioSelectionViewModel,
    onSelectedAudio: (AudioResponse) -> Unit,
    onDismissBottomSheet: () -> Unit,
) {
    val audios by audioSelectionViewModel.audios.collectAsState()
    val previewAudio by audioSelectionViewModel.onPreviewSelectedAudio.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val lifeCycleOwner = LocalLifecycleOwner.current

    var isAudioPlaying by rememberSaveable {
        mutableStateOf(false)
    }

    // Playing audio instance
    val exoPlayer =
        rememberExoPlayer(
            context = context,
            onVideoStateChanged = { state ->
                when (state) {
                    Player.STATE_ENDED -> {
                        isAudioPlaying = false
                    }

                    Player.STATE_READY -> {
                        isAudioPlaying = true
                    }
                }
            })

    LaunchedEffect(previewAudio) {
        if (previewAudio == null) {
            isAudioPlaying = false
            exoPlayer.pause()
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(previewAudio!!.audioUrl)))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (isAudioPlaying) {
                        exoPlayer.play()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (isAudioPlaying) {
                        exoPlayer.pause()
                    }
                }

                else -> {

                }
            }
        }

        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            exoPlayer.release()
            audioSelectionViewModel.onSelectToPreview(null)
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismissBottomSheet.invoke() },
        sheetState = sheetState
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(audios) {
                AudioItemView(
                    isAudioPlaying = previewAudio?.audioUrl == it.audioUrl && isAudioPlaying,
                    audioResponse = it,
                    onActionClick = { audio ->
                        if (audio.audioUrl == previewAudio?.audioUrl) {
                            audioSelectionViewModel.onSelectToPreview(null)
                        } else {
                            audioSelectionViewModel.onSelectToPreview(it)
                        }
                    }, onSelectAudio = { audio ->
                        onSelectedAudio.invoke(audio)
                    })
            }
        }
    }
}

@UnstableApi
@Composable
private fun rememberExoPlayer(
    context: Context,
    onVideoStateChanged: (Int) -> Unit,
): ExoPlayer {
    return remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    onVideoStateChanged.invoke(playbackState)
                }
            })
        }
    }
}

@Composable
@Preview
private fun AudioSelectionBottomSheetPreview() {
    AudioSelectionBottomSheet(audioSelectionViewModel = hiltViewModel(), onSelectedAudio = {

    }) {

    }
}
package com.densitech.scrollsmooth.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.video_transformation.model.AudioResponse
import com.densitech.scrollsmooth.ui.video_transformation.viewmodel.GetAudiosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioSelectionViewModel @Inject constructor(
    private val getAudiosUseCase: GetAudiosUseCase = GetAudiosUseCase(),
) : ViewModel() {

    private val _audios: MutableStateFlow<List<AudioResponse>> = MutableStateFlow(emptyList())
    val audios = _audios.asStateFlow()

    private val _onPreviewSelectedAudio: MutableStateFlow<AudioResponse?> = MutableStateFlow(null)
    val onPreviewSelectedAudio = _onPreviewSelectedAudio.asStateFlow()

    private val _onSelectedAudio: MutableStateFlow<AudioResponse?> = MutableStateFlow(null)
    val onSelectedAudio = _onSelectedAudio.asStateFlow()

    init {
        viewModelScope.launch {
            fetchAudios()
        }
    }

    fun onSelectAudio(audio: AudioResponse) {
        _onSelectedAudio.value = audio
    }

    fun onSelectToPreview(audio: AudioResponse?) {
        _onPreviewSelectedAudio.value = audio
    }

    private suspend fun fetchAudios() {
        val audios = getAudiosUseCase.fetchAudios()
        _audios.value = audios
    }
}
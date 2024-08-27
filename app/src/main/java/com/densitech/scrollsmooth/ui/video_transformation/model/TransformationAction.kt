package com.densitech.scrollsmooth.ui.video_transformation.model

import com.densitech.scrollsmooth.R

sealed class TransformationAction(val key: String, val iconId: Int) {
    data object Music : TransformationAction(key = "Music", iconId = R.drawable.ic_library_music)
    data object Text : TransformationAction(key = "Text", iconId = R.drawable.ic_match_case)
    data object Sticker :
        TransformationAction(key = "Sticker", iconId = R.drawable.ic_sticker)
}
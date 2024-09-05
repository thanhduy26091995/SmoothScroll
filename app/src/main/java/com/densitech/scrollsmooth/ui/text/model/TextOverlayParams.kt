package com.densitech.scrollsmooth.ui.text.model

import androidx.compose.ui.graphics.Color
import com.densitech.scrollsmooth.ui.text.view.TextAlignmentEnum
import java.util.UUID

data class TextOverlayParams(
    val key: String = UUID.randomUUID().toString(),
    val text: String,
    val fontSize: Float,
    val textColor: Color,
    val font: String,
    val textAlignment: TextAlignmentEnum,
)

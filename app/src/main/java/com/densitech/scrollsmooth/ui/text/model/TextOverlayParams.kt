package com.densitech.scrollsmooth.ui.text.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class TextOverlayParams(
    val key: String = UUID.randomUUID().toString(),
    val text: String,
    val fontSize: Float,
    val textColor: Color,
    val font: String,
    val textX: Float,
    val textY: Float,
    val scale: Float,
    val rotationAngle: Float
)

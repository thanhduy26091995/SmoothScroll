package com.densitech.scrollsmooth.ui.text.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class TextOverlayParams(
    val key: String = UUID.randomUUID().toString(),
    val text: String,
    val fontSize: Float,
    val textColor: Color,
    val font: String,
    var textX: Float,
    var textY: Float,
    var scale: Float,
    var rotationAngle: Float
)

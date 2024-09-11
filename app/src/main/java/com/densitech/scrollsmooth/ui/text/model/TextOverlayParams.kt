package com.densitech.scrollsmooth.ui.text.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
    var rotationAngle: Float,
    var textAlignment: TextAlignmentEnum,
) {
    companion object {
        fun default(): TextOverlayParams {
            return TextOverlayParams(
                key = "",
                text = "",
                fontSize = 48f,
                textColor = Color.White,
                font = FontFamily.Monospace.name,
                textX = Offset.Zero.x,
                textY = Offset.Zero.y,
                scale = 1f,
                rotationAngle = 0f,
                textAlignment = TextAlignmentEnum.CENTER
            )
        }
    }
}

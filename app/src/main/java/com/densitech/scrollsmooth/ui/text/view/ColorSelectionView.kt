package com.densitech.scrollsmooth.ui.text.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
fun ColorSelectionView(modifier: Modifier = Modifier, onSelectedColor: (Color) -> Unit) {
    val colors = arrayListOf(Color.White)
    for (i in 0..20) {
        colors.add(generateColor())
    }

    LazyRow(modifier = modifier) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(24.dp)
                    .border(1.dp, Color.White, CircleShape)
                    .background(color)
                    .clickableNoRipple {
                        onSelectedColor.invoke(color)
                    }
            )

            Spacer(modifier = Modifier.width(5.dp))
        }
    }
}

private fun generateColor(): Color {
    val random = java.util.Random()
    val red = random.nextInt(256)
    val green = random.nextInt(256)
    val blue = random.nextInt(256)
    return Color(red, green, blue)
}
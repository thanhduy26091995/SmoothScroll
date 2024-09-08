package com.densitech.scrollsmooth.ui.text.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
@Preview
private fun FontPreviewItemViewPreview() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = "Aa",
            fontSize = 14.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun FontPreviewView(
    fonts: List<String>,
    selectedFont: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(modifier = modifier) {
        items(fonts) {
            FontPreviewItemView(
                isSelect = selectedFont == it,
                fontFamily = it,
                onFontSelected = onFontSelected
            )

            Spacer(modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
private fun FontPreviewItemView(
    isSelect: Boolean,
    fontFamily: String,
    onFontSelected: (String) -> Unit,
) {
    val background = if (isSelect) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.2f)
    }

    val textColor = if (isSelect) {
        Color.Red
    } else {
        Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(background)
            .padding(12.dp)
            .clickableNoRipple {
                onFontSelected.invoke(fontFamily)
            }
    ) {
        Text(
            text = "Aa",
            fontSize = 14.sp,
            color = textColor,
            fontFamily = stringToFont(fontFamily)
        )
    }
}

fun stringToFont(fontName: String): FontFamily {
    return when (fontName) {
        FontFamily.Serif.name -> FontFamily.Serif
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.Cursive.name -> FontFamily.Cursive
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

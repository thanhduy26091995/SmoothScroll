package com.densitech.scrollsmooth.ui.bottom_sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import kotlin.math.abs

@Composable
fun SheetContent(
    modifier: Modifier = Modifier,
    heightFraction: Float = DEFAULT_FRACTION,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = heightFraction)
    ) {
        content()
    }
}

@Composable
fun SheetExpanded(
    currentFraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (currentFraction < 0) {
        return
    }
    val alpha = when {
        currentFraction == 0f -> 0f
        abs(DEFAULT_FRACTION - currentFraction) <= 0.1 -> 1f
        else -> 1f - (DEFAULT_FRACTION + currentFraction)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = alpha)
    ) {
        content()
    }
}

@Composable
fun SheetCollapsed(
    currentFraction: Float,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    // Hide view in case alpha = 0
    if (currentFraction <= 0.01) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
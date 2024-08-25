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
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple
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
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
    }
}

@Composable
fun SheetCollapsed(
    isCollapsed: Boolean,
    currentFraction: Float,
    onSheetClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val alpha = when {
        currentFraction == 0f -> 1f
        abs(DEFAULT_FRACTION - currentFraction) <= 0.1 -> 0f
        else -> 1f - (DEFAULT_FRACTION + currentFraction)
    }

    Row(
        modifier = modifier
            .graphicsLayer(alpha = alpha)
            .clickableNoRipple(
                onClick = onSheetClick,
                enabled = isCollapsed
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}
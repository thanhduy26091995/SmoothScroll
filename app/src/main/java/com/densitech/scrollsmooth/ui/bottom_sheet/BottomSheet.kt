package com.densitech.scrollsmooth.ui.bottom_sheet

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.densitech.scrollsmooth.ui.utils.DEFAULT_FRACTION
import com.densitech.scrollsmooth.ui.utils.SMALL_FRACTION_TO_IGNORE
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
    if (currentFraction < SMALL_FRACTION_TO_IGNORE) {
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
    if (currentFraction <= SMALL_FRACTION_TO_IGNORE) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
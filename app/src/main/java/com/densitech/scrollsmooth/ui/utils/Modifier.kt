package com.densitech.scrollsmooth.ui.utils

import android.annotation.SuppressLint
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.*
import androidx.compose.ui.unit.*

fun Modifier.fixedPadding() = this then FixedPaddingElement

// ZERO PARAMETER
@SuppressLint("ModifierNodeInspectableProperties")
data object FixedPaddingElement : ModifierNodeElement<FixedPaddingNode>() {
    override fun create(): FixedPaddingNode {
        return FixedPaddingNode()
    }

    override fun update(node: FixedPaddingNode) {
    }
}

class FixedPaddingNode : LayoutModifierNode, Modifier.Node() {
    private val defaultPadding = 16.dp

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val paddingPx = defaultPadding.roundToPx()
        val horizontal = paddingPx * 2
        val vertical = paddingPx * 2

        val placeable =
            measurable.measure(constraints.offset(horizontal = -horizontal, vertical = -vertical))
        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + horizontal)
        return layout(width, height) {
            placeable.place(paddingPx, paddingPx)
        }
    }
}

// Referencing composition locals
class BackgroundColorConsumerNode : Modifier.Node(), DrawModifierNode,
    CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        val currentColor = currentValueOf(LocalContentColor)
        drawRect(currentColor)
        drawContent()
    }
}

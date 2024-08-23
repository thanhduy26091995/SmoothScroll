package com.densitech.scrollsmooth.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier

fun Modifier.clickableNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    return this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        enabled = enabled
    ) {
        onClick()
    }
}

fun Float.format(scale: Int) = "%.${scale}f".format(this)
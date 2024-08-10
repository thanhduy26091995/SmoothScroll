package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderTimeView(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    onDraggedSlider: (Boolean) -> Unit,
    onTempSliderPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderPosition = remember(currentValue) { mutableFloatStateOf(currentValue) }
    val tempSliderPosition = remember { mutableFloatStateOf(currentValue) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged = interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged.value) {
        onDraggedSlider.invoke(isDragged.value)
    }

    Slider(
        modifier = modifier,
        onValueChange = { progress ->
            sliderPosition.floatValue = progress
            tempSliderPosition.floatValue = progress

            onTempSliderPositionChange.invoke(tempSliderPosition.floatValue)
        },
        thumb = {

        },
        value = if (isDragged.value) tempSliderPosition.floatValue else sliderPosition.floatValue,
        onValueChangeFinished = {
            sliderPosition.floatValue = tempSliderPosition.floatValue
            onValueChange(tempSliderPosition.floatValue)
        },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTickColor = Color.Gray
        )
    )
}

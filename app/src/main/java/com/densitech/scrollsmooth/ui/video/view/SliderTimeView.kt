package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.R

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(10.dp)
                    .background(Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_comment_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center)
                        .background(Color.White, CircleShape)
                )
            }
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

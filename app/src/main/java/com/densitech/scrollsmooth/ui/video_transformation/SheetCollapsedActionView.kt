package com.densitech.scrollsmooth.ui.video_transformation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SheetCollapsedActionView(
    onEditVideoClick: () -> Unit,
    onNextVideoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            onClick = {
                onEditVideoClick.invoke()
            },
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
        ) {
            Text("Edit Video", fontSize = 12.sp)
        }

        TextButton(
            onClick = {
                onNextVideoClick.invoke()
            },
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Blue),
            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 24.dp)
        ) {
            Text("Next")
        }
    }
}
package com.densitech.scrollsmooth.ui.video.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.utils.formatTime

@Composable
fun SeekingTimeView(currentPosition: Long, duration: Long, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatTime(currentPosition), fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = " / ", fontSize = 16.sp)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = formatTime(duration), fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
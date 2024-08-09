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
import java.util.Locale

@Composable
fun SeekingTimeView(currentPosition: Long, duration: Long, modifier: Modifier = Modifier) {
    val timeFormat = "%02d:%02d"

    fun formatTime(timeInMilliSeconds: Long): String {
        val timeInSeconds = (timeInMilliSeconds / 1000)
        val minutes = timeInSeconds / 60
        val seconds = timeInSeconds % 60
        return String.format(Locale.getDefault(), timeFormat, minutes, seconds)
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatTime(currentPosition), fontSize = 28.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = " / ", fontSize = 16.sp)

        Spacer(modifier = Modifier.width(5.dp))

        Text(text = formatTime(duration), fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}
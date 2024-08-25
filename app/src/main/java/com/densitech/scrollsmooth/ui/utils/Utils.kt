package com.densitech.scrollsmooth.ui.utils

import java.util.Locale

const val DEFAULT_FRACTION = 0.4f

fun formatTime(timeInMilliSeconds: Long, timeFormat: String = "%02d:%02d"): String {
    val timeInSeconds = (timeInMilliSeconds / 1000)
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60
    return String.format(Locale.getDefault(), timeFormat, minutes, seconds)
}
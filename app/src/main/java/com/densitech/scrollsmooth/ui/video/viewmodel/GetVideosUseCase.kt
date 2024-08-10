package com.densitech.scrollsmooth.ui.video.viewmodel

import com.densitech.scrollsmooth.ui.video.model.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class GetVideosUseCase @Inject constructor() {
    suspend fun fetchVideos(): List<MediaInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl =
                    "https://firebasestorage.googleapis.com/v0/b/smoothscroll-7252a.appspot.com/o/video_list.json?alt=media&token=77339fd9-3ad3-44f0-9aa4-b529f91c1c36"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection

                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    Json.decodeFromString(response)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
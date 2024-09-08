package com.densitech.scrollsmooth.ui.video_transformation.viewmodel

import com.densitech.scrollsmooth.ui.video_transformation.model.AudioResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/*
*@Author: Dennis
*Description: A use case to fetch list of audio to play in the video
* */
class GetAudiosUseCase @Inject constructor() {
    suspend fun fetchAudios(): List<AudioResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val apiUrl =
                    "https://firebasestorage.googleapis.com/v0/b/smoothscroll-7252a.appspot.com/o/audio_metadata.json?alt=media&token=701aa8b2-4b41-4cca-b401-ab1bd0f1ac54"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
                connection.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()

                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = true
                    }
                    json.decodeFromString(response)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
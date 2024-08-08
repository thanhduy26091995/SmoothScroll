package com.densitech.scrollsmooth.ui.video.preFetch

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class NetworkType {
    WIFI, CELLULAR, ETHERNET, OTHER
}

class BitrateManager(private val context: Context) {
    private val bitrateMap = mutableMapOf<NetworkType, Long>()

    fun getLatestKnowBitrateByNetworkType(): Long {
        val networkType = getCurrentNetworkType()
        return bitrateMap[networkType] ?: getDefaultBitrateForNetworkType(networkType)
    }

    private fun getCurrentNetworkType(): NetworkType {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetworkType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetworkType.CELLULAR
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    private fun getDefaultBitrateForNetworkType(networkType: NetworkType): Long {
        return when (networkType) {
            NetworkType.WIFI -> 5_000_000L // Default 5 Mbps for Wi-Fi
            NetworkType.CELLULAR -> 2_000_000L // Default 2 Mbps for Cellular
            NetworkType.ETHERNET -> 10_000_000L // Default 10 Mbps for Ethernet
            NetworkType.OTHER -> 1_000_000L // Default 1 Mbps for other/unknown types
        }
    }

    fun updateBitrateForNetworkType(networkType: NetworkType, bitrate: Long) {
        bitrateMap[networkType] = bitrate
    }
}
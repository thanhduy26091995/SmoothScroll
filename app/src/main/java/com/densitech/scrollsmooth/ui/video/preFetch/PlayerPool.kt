package com.densitech.scrollsmooth.ui.video.preFetch

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.util.EventLogger
import com.densitech.scrollsmooth.ui.utils.CustomAnalyticsListener
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Maps
import org.chromium.net.CronetEngine
import java.io.File
import java.util.Collections
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
class PlayerPool(
    private val numberOfPlayers: Int,
    context: Context,
    playbackLooper: Looper,
    loadControl: LoadControl,
    renderersFactory: RenderersFactory,
    bandwidthMeter: BandwidthMeter
) {
    /** Creates a player instance to be used by the pool. */
    interface PlayerFactory {
        fun createPlayer(): ExoPlayer
    }

    private val availablePlayerQueue: Queue<Int> = LinkedList()
    private val playerMap: BiMap<Int, ExoPlayer> = Maps.synchronizedBiMap(HashBiMap.create())
    private val playerRequestTokenSet: MutableSet<Int> = Collections.synchronizedSet(HashSet())
    private val playerFactory: PlayerFactory =
        DefaultPlayerFactory(context, playbackLooper, loadControl, renderersFactory, bandwidthMeter)

    fun acquirePlayer(token: Int, callback: (ExoPlayer) -> Unit) {
        synchronized(playerMap) {
            // Add token to set of views requesting players
            playerRequestTokenSet.add(token)
            acquirePlayerInternal(token, callback)
        }
    }

    private fun acquirePlayerInternal(token: Int, callback: (ExoPlayer) -> Unit) {
        synchronized(playerMap) {
            if (!availablePlayerQueue.isEmpty()) {
                val playerNumber = availablePlayerQueue.remove()
                playerMap[playerNumber]?.let { callback.invoke(it) }
                playerRequestTokenSet.remove(token)
                return
            } else if (playerMap.size < numberOfPlayers) {
                val player = playerFactory.createPlayer()
                playerMap[playerMap.size] = player
                callback.invoke(player)
                playerRequestTokenSet.remove(token)
                return
            } else if (playerRequestTokenSet.contains(token)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    acquirePlayerInternal(
                        token,
                        callback
                    )
                }, 500)
            }
        }
    }

    fun play(player: Player) {
        pauseAllPlayer(player)
        player.play()
    }

    fun releasePlayer(token: Int, player: ExoPlayer?) {
        synchronized(playerMap) {
            // Remove token from set of views requesting players & remove potential callbacks
            // trying to grab the player

            playerRequestTokenSet.remove(token)
            player?.stop()
            player?.clearMediaItems()

            if (player != null) {
                val playerNumber = playerMap.inverse()[player]
                availablePlayerQueue.add(playerNumber)
            }
        }
    }

    fun destroyPlayer() {
        synchronized(playerMap) {
            for (i in 0 until playerMap.size) {
                playerMap[i]?.release()
                playerMap.remove(i)
            }
        }
    }

    private fun pauseAllPlayer(keepOnGoingPlayer: Player? = null) {
        playerMap.values.forEach {
            if (it != keepOnGoingPlayer) {
                it.pause()
            }
        }
    }

    private class DefaultPlayerFactory(
        private val context: Context,
        private val playbackLooper: Looper,
        private val loadControl: LoadControl,
        private val renderersFactory: RenderersFactory,
        private val bandwidthMeter: BandwidthMeter
    ) : PlayerFactory {
        private var playerCounter = 0

        override fun createPlayer(): ExoPlayer {
            val cache = CacheSingleton.getInstance(context)
            val cacheSink = CacheDataSink.Factory().setCache(cache)

            // Cronet
            val cronetEngine = CronetEngine.Builder(context).build()
            val cronetDataSourceFactory = CronetDataSource.Factory(
                cronetEngine,
                Executors.newSingleThreadExecutor()
            )

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setCacheWriteDataSinkFactory(cacheSink)
                .setUpstreamDataSourceFactory(cronetDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)


            val player = ExoPlayer.Builder(context)
                .setPlaybackLooper(playbackLooper)
                .setLoadControl(loadControl)
                .setRenderersFactory(renderersFactory)
                .setBandwidthMeter(bandwidthMeter)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context).setDataSourceFactory(
                        cacheDataSourceFactory
                    )
                )
                .build()
            player.addAnalyticsListener(EventLogger("player-${playerCounter}"))
            player.addAnalyticsListener(CustomAnalyticsListener())
            playerCounter++
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            return player
        }

        @Synchronized
        private fun getDownloadCache(context: Context): Cache {
            val databaseProvider = StandaloneDatabaseProvider(context)
            val downloadDirectory = File(context.cacheDir, "media_cache")
            if (!downloadDirectory.exists()) {
                val created = downloadDirectory.mkdirs()
                if (!created) {
                    throw IllegalStateException("Failed to create cache directory: ${downloadDirectory.absolutePath}")
                }
            }
//            val cacheDir = File(Util.createTempFile(context, "CacheDir"), "cache")
//            if (!cacheDir.exists()) {
//                cacheDir.mkdirs()
//            }
            val maxBytes = 50 * 1024 * 1024L
            val cache =
                SimpleCache(
                    downloadDirectory,
                    LeastRecentlyUsedCacheEvictor(maxBytes),
                    databaseProvider
                )
            return cache
        }
    }
}


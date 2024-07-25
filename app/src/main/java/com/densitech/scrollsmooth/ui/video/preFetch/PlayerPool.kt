package com.densitech.scrollsmooth.ui.video.preFetch

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.util.EventLogger
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Maps
import java.util.Collections
import java.util.LinkedList
import java.util.Queue

@SuppressLint("UnsafeOptInUsageError")
class PlayerPool
    (
    private val numberOfPlayers: Int,
    private val context: Context,
    private val playbackLooper: Looper,
    private val loadControl: LoadControl,
    private val renderersFactory: RenderersFactory,
    private val bandwidthMeter: BandwidthMeter
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

    private class DefaultPlayerFactory(
        private val context: Context,
        private val playbackLooper: Looper,
        private val loadControl: LoadControl,
        private val renderersFactory: RenderersFactory,
        private val bandwidthMeter: BandwidthMeter
    ) : PlayerFactory {
        private var playerCounter = 0

        override fun createPlayer(): ExoPlayer {
            val player = ExoPlayer.Builder(context)
                .setPlaybackLooper(playbackLooper)
                .setLooper(playbackLooper)
                .setLoadControl(loadControl)
                .setRenderersFactory(renderersFactory)
                .setBandwidthMeter(bandwidthMeter)
                .build()
            player.addAnalyticsListener(EventLogger("player-${playerCounter}"))
            playerCounter++
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            return player
        }
    }
}
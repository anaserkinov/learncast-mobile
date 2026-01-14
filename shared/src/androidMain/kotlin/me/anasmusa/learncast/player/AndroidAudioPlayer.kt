package me.anasmusa.learncast.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.DownloadCacheScope
import me.anasmusa.learncast.PlaybackCacheScope
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.network.TokenManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

@UnstableApi
internal class AndroidAudioPlayer(
    context: Context,
    audioPath: String,
    startPosition: Long,
) : AudioPlayer,
    KoinComponent {
    override val playbackState = MutableStateFlow(STATE_LOADING)

    private val exoPlayer =
        ExoPlayer
            .Builder(
                context,
                ProgressiveMediaSource.Factory(
                    createCacheDataSourceFactory(
                        downloadCache = get(named(DownloadCacheScope.ID)),
                        playbackCache = get(named(PlaybackCacheScope.ID)),
                        httpDataSourceFactory = HttpDataSourceFactory(get<TokenManager>()),
                    ),
                ),
            ).build()

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onEvents(
                    player: Player,
                    events: Player.Events,
                ) {
                    super.onEvents(player, events)
                    when {
                        events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) -> {
                            playbackState.value =
                                if (player.playbackState == Player.STATE_BUFFERING) {
                                    STATE_LOADING
                                } else if (player.isPlaying) {
                                    STATE_PLAYING
                                } else {
                                    STATE_PAUSED
                                }
                        }

                        events.contains(Player.EVENT_IS_PLAYING_CHANGED) -> {
                            playbackState.value =
                                if (player.isPlaying) {
                                    STATE_PLAYING
                                } else {
                                    STATE_PAUSED
                                }
                        }
                    }
                }
            },
        )

        exoPlayer.playWhenReady = false
        exoPlayer.setMediaItem(
            MediaItem
                .Builder()
                .setUri(audioPath.normalizeUrl())
                .build(),
            startPosition,
        )
        exoPlayer.prepare()
    }

    override fun getCurrentPositonMs(): Long = exoPlayer.currentPosition

    override fun start(from: Long) {
        exoPlayer.playWhenReady = true
        exoPlayer.seekTo(from)
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
    }

    override fun stop() {
        exoPlayer.pause()
    }

    override fun destroy() {
        exoPlayer.release()
    }
}

@OptIn(UnstableApi::class)
internal actual fun createAudioPlayer(
    audioPath: String,
    startPosition: Long,
): AudioPlayer = AndroidAudioPlayer(ApplicationLoader.context, audioPath, startPosition)

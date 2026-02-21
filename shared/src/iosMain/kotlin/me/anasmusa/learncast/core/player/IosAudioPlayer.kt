package me.anasmusa.learncast.core.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.core.player.avplayer.AVPlayerCallback
import me.anasmusa.learncast.core.player.avplayer.AVPlayerDelegate
import me.anasmusa.learncast.core.player.avplayer.ItemTransitionReason
import me.anasmusa.learncast.core.player.avplayer.PlaybackState
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.TokenManager
import me.anasmusa.learncast.data.network.TokenProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private class IosAudioPlayer(
    audioPath: String,
    startPosition: Long,
) : AudioPlayer,
    KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var playerDelegate = AVPlayerDelegate.factory.invoke()
    private val tokenManager by inject<TokenManager>()

    override val playbackState = MutableStateFlow(STATE_LOADING)

    init {
        playerDelegate.setCallback(
            object : AVPlayerCallback {
                override fun onPlaybackStateChanged(state: PlaybackState) {
                    playbackState.value =
                        if (state == PlaybackState.STATE_BUFFERING) {
                            STATE_LOADING
                        } else if (playerDelegate.isPlaying()) {
                            STATE_PLAYING
                        } else {
                            STATE_PAUSED
                        }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackState.value =
                        if (playerDelegate.isPlaying()) {
                            STATE_PLAYING
                        } else {
                            STATE_PAUSED
                        }
                }

                override fun onItemTransition(
                    oldItem: QueueItem?,
                    oldPositionMs: Long,
                    newItem: QueueItem?,
                    reason: ItemTransitionReason,
                ) {}

                override fun onPlaybackResumption() {}
            },
        )

        playerDelegate.setTokenProvider(
            object : TokenProvider {
                override fun getTokens(): Pair<String, String>? = runBlocking { tokenManager.getTokens() }

                override fun refreshTokens(refreshToken: String) {
                    runBlocking {
                        scope
                            .launch {
                                tokenManager.refreshToken(refreshToken)
                            }.join()
                    }
                }
            },
        )

        playerDelegate.setPlayInBackground(false)
        playerDelegate.setPlayWhenReady(false)
        playerDelegate.setItem(
            item =
                QueueItem(
                    id = 1,
                    referenceId = 1L,
                    referenceUuid = "",
                    referenceType = ReferenceType.LESSON,
                    startMs = null,
                    endMs = null,
                    lessonId = 1L,
                    title = "",
                    description = null,
                    coverImagePath = null,
                    authorId = 1L,
                    authorName = "",
                    topicId = null,
                    topicTitle = null,
                    audioPath = audioPath,
                    audioSize = 0L,
                    audioDuration = 0.toDuration(DurationUnit.MILLISECONDS),
                    lastPositionMs = 0L,
                    status = UserProgressStatus.NOT_STARTED,
                    isFavourite = false,
                    downloadState = null,
                    percentDownloaded = 0f,
                ),
            startPosition = startPosition,
        )
    }

    override fun getCurrentPositonMs(): Long = playerDelegate.getCurrentPosition()

    override fun start(from: Long) {
        playerDelegate.setPlayWhenReady(true)
        playerDelegate.seekTo(from)
        playerDelegate.play()
    }

    override fun stop() {
        playerDelegate.pause()
    }

    override fun destroy() {
        playerDelegate.release()
        scope.cancel()
    }
}

internal actual fun createAudioPlayer(
    audioPath: String,
    startPosition: Long,
): AudioPlayer = IosAudioPlayer(audioPath = audioPath, startPosition = startPosition)

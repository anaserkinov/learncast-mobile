package me.anasmusa.learncast.player

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.DownloadCacheScope
import me.anasmusa.learncast.PlaybackCacheScope
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.data.toMediaItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@UnstableApi
class PlaybackService :
    MediaLibraryService(),
    KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val queueRepository by inject<QueueRepository>()
    private val outboxRepository by inject<OutboxRepository>()

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    private val mediaItemId = MutableStateFlow<String?>(null)
    private val isPlaying = MutableStateFlow(false)

    private var lastMediaItemsId: String? = null
    private var listenedS = 0

    private val skipBackButton =
        CommandButton
            .Builder(CommandButton.ICON_SKIP_BACK_10)
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setDisplayName("Skip")
            .setSlots(CommandButton.SLOT_BACK)
            .build()
    private val playPause =
        CommandButton
            .Builder(CommandButton.ICON_PLAY)
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setDisplayName("Play")
            .setSlots(CommandButton.SLOT_CENTRAL)
            .build()
    private val skipForwardButton =
        CommandButton
            .Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setDisplayName("Skip")
            .setSlots(CommandButton.SLOT_FORWARD)
            .build()

    private var callback: MediaLibrarySession.Callback =
        @UnstableApi object : MediaLibrarySession.Callback {
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                isForPlayback: Boolean,
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                scope.launch {
                    val queuedItems = queueRepository.getQueuedItems()
                    val mediaItems = queuedItems.map { it.toMediaItem(this@PlaybackService) }
                    settable.set(
                        MediaSession.MediaItemsWithStartPosition(
                            mediaItems,
                            0,
                            queuedItems.getOrNull(0)?.lastPositionMs?.inWholeMilliseconds ?: 0L,
                        ),
                    )
                }
                return settable
            }

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult =
                MediaSession.ConnectionResult
                    .AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                            .buildUpon()
                            .add(SessionCommand("destroy_player", Bundle.EMPTY))
                            .build(),
                    ).build()

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle,
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == "destroy_player" && controller.packageName == packageName) {
                    player?.let { player ->
                        if (player.currentPosition > 0L) {
                            player.currentMediaItem?.let {
                                updateListenProgress(
                                    queueItemId = it.mediaId.toLong(),
                                    position = player.currentPosition,
                                )
                            }
                        }
                        player.clearMediaItems()
                        player.release()
                    }
                    mediaLibrarySession?.release()
                    mediaLibrarySession = null
                    player = null
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<MediaItem>> = super.onGetLibraryRoot(session, browser, params)

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = super.onGetChildren(session, browser, parentId, page, pageSize, params)

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<MediaItem>> = super.onGetItem(session, browser, mediaId)

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = super.onGetSearchResult(session, browser, query, page, pageSize, params)
        }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        if (mediaLibrarySession == null) {
            val cacheDataSourceFactory =
                createCacheDataSourceFactory(
                    downloadCache = get(named(DownloadCacheScope.ID)),
                    playbackCache = get(named(PlaybackCacheScope.ID)),
                    httpDataSourceFactory = HttpDataSourceFactory(get()),
                )

            val player =
                ExoPlayer
                    .Builder(
                        this,
                        DefaultMediaSourceFactory(cacheDataSourceFactory),
                    ).setSeekForwardIncrementMs(30_000)
                    .setSeekBackIncrementMs(10_000)
                    .build()

            player.addAnalyticsListener(EventLogger())

            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        super.onIsPlayingChanged(isPlaying)
                        if (
                            !isPlaying &&
                            this@PlaybackService.isPlaying.value &&
                            player.playbackState == Player.STATE_READY &&
                            mediaItemId.value == player.currentMediaItem?.mediaId &&
                            player.currentPosition > 0L
                        ) {
                            player.currentMediaItem?.let {
                                updateListenProgress(
                                    queueItemId = it.mediaId.toLong(),
                                    position = player.currentPosition,
                                )
                            }
                        }
                        this@PlaybackService.isPlaying.update { isPlaying }
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        super.onMediaItemTransition(mediaItem, reason)
                        if (mediaItem != null && mediaItemId.value != mediaItem.mediaId) {
                            scope.launch {
                                val queueItem = queueRepository.getById(mediaItem.mediaId.toLong()) ?: return@launch
                                withContext(Dispatchers.Main) {
                                    if (queueItem.id == player.currentMediaItem?.mediaId?.toLong()) {
                                        queueItem.lastPositionMs?.inWholeMilliseconds?.let {
                                            player.seekTo(it)
                                        }
                                    }
                                }
                            }
                        }
                        mediaItemId.update { mediaItem?.mediaId }
                        if (player.currentMediaItemIndex > 0) {
                            player.removeMediaItem(0)
                        }

                        if (mediaItem != null) {
                            scope.launch {
                                queueRepository.ensureItemIsFirst(mediaItem.mediaId.toLong())
                            }
                        }
                    }

                    override fun onPositionDiscontinuity(
                        oldPosition: Player.PositionInfo,
                        newPosition: Player.PositionInfo,
                        reason: Int,
                    ) {
                        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                        if (
                            oldPosition.mediaItem?.mediaId != null &&
                            oldPosition.mediaItem?.mediaId != newPosition.mediaItem?.mediaId
                        ) {
                            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                                updateListenProgress(
                                    queueItemId = oldPosition.mediaItem!!.mediaId.toLong(),
                                    position = 0L,
                                    status = UserProgressStatus.COMPLETED,
                                    completedAt = nowLocalDateTime(),
                                )
                            } else if (oldPosition.positionMs > 0) {
                                updateListenProgress(
                                    queueItemId = oldPosition.mediaItem!!.mediaId.toLong(),
                                    position = oldPosition.positionMs,
                                )
                            }
                        }
                    }
                },
            )

            this.player = player
            mediaLibrarySession =
                MediaLibrarySession
                    .Builder(this, player, callback)
                    .setBitmapLoader(CacheBitmapLoader(BitmapLoader(this, scope)))
                    .setMediaButtonPreferences(
                        ImmutableList.of(
                            skipBackButton,
                            playPause,
                            skipForwardButton,
                        ),
                    ).build()
        }
        return mediaLibrarySession!!
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val flow =
                isPlaying.combine(mediaItemId) { isPlaying, mediaItemId ->
                    Pair(isPlaying, mediaItemId)
                }
            launch {
                flow.collectLatest {
                    if (lastMediaItemsId != it.second) {
                        lastMediaItemsId = it.second
                        listenedS = 0
                    }
                    if (it.second == null) return@collectLatest
                    while (it.first && listenedS < 60) {
                        delay(1000)
                        yield()
                        listenedS++
                        if (listenedS == 60) {
                            queueRepository.getLessonId(it.second!!.toLong())?.let { lessonId ->
                                outboxRepository.listen(lessonId)
                            }
                        }
                    }
                }
            }
            launch {
                flow.collectLatest {
                    while (it.first && it.second != null) {
                        delay(30_000)
                        withContext(Dispatchers.Main) {
                            player?.let { player ->
                                if (player.currentMediaItem?.mediaId == it.second) {
                                    updateListenProgress(
                                        queueItemId = it.second!!.toLong(),
                                        position = player.currentPosition,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateListenProgress(
        queueItemId: Long,
        position: Long,
        status: UserProgressStatus? = null,
        completedAt: LocalDateTime? = null,
    ) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) {
                queueRepository.getLessonId(queueItemId)?.let { lessonId ->
                    outboxRepository.updateLessonProgress(
                        lessonId = lessonId,
                        startedAt = nowLocalDateTime(),
                        lastPositionMs = position.toDuration(DurationUnit.MILLISECONDS),
                        status = status,
                        completedAt = completedAt,
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        scope.cancel()
        super.onDestroy()
    }
}

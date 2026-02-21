package me.anasmusa.learncast.core.player

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.core.player.avplayer.AVPlayerCallback
import me.anasmusa.learncast.core.player.avplayer.AVPlayerDelegate
import me.anasmusa.learncast.core.player.avplayer.ItemTransitionReason
import me.anasmusa.learncast.core.player.avplayer.PlaybackState
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.TokenManager
import me.anasmusa.learncast.data.network.TokenProvider
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.max
import kotlin.math.min
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IOSPlayerController :
    PlayerController,
    KoinComponent {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val queueRepository by inject<QueueRepository>()
    private val outboxRepository by inject<OutboxRepository>()
    private val tokenManager by inject<TokenManager>()

    private var playerDelegate: AVPlayerDelegate? = AVPlayerDelegate.factory.invoke()

    override val currentQueueItemId = MutableStateFlow<Long?>(null)
    override val playbackState = MutableStateFlow(STATE_LOADING)

    private val itemId = MutableStateFlow<Long?>(null)
    private val isPlaying = MutableStateFlow(false)

    private var wasPlaying = false
    private var isStopped = false
    private var lastReturnedPositionMs: Long = 0L
    private var lastMediaItemsId: Long? = null
    private var listenedS = 0
    private var listenTrackingJob: Job? = null

    init {
        setup()
    }

    private fun setup() {
        playerDelegate?.setCallback(
            object : AVPlayerCallback {
                override fun onPlaybackStateChanged(state: PlaybackState) {
                    playbackState.value =
                        if (state == PlaybackState.STATE_BUFFERING) {
                            STATE_LOADING
                        } else if (playerDelegate?.isPlaying() == true) {
                            STATE_PLAYING
                        } else {
                            STATE_PAUSED
                        }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackState.value =
                        if (isPlaying) {
                            STATE_PLAYING
                        } else {
                            STATE_PAUSED
                        }

                    if (
                        !isPlaying &&
                        this@IOSPlayerController.isPlaying.value &&
                        playerDelegate?.playbackState() == PlaybackState.STATE_READY &&
                        itemId.value == playerDelegate?.currentItem()?.id &&
                        (playerDelegate?.getCurrentPosition() ?: 0L) > 0L
                    ) {
                        playerDelegate?.currentItem()?.let {
                            updateListenProgress(
                                queueItemId = it.id,
                                position = playerDelegate?.getCurrentPosition() ?: 0,
                            )
                        }
                    }
                    this@IOSPlayerController.isPlaying.update { isPlaying }
                }

                override fun onItemTransition(
                    oldItem: QueueItem?,
                    oldPositionMs: Long,
                    newItem: QueueItem?,
                    reason: ItemTransitionReason,
                ) {
                    if (isStopped) return
                    currentQueueItemId.value = newItem?.id

                    if (newItem != null && itemId.value != newItem.id && newItem.referenceType == ReferenceType.LESSON) {
                        scope.launch {
                            val queueItem = queueRepository.getById(newItem.id) ?: return@launch
                            withContext(Dispatchers.Main) {
                                if (queueItem.id == playerDelegate?.currentItem()?.id) {
                                    playerDelegate?.seekTo(queueItem.lastPositionMs)
                                }
                            }
                        }
                    }
                    itemId.update { newItem?.id }
                    if ((playerDelegate?.getCurrentItemIndex() ?: 0) > 0) {
                        playerDelegate?.removeItem(0)
                    }

                    if (newItem != null) {
                        scope.launch {
                            queueRepository.ensureItemIsFirst(newItem.id)
                        }
                    }

                    if (
                        oldItem != null &&
                        oldItem.id != newItem?.id
                    ) {
                        if (reason == ItemTransitionReason.AUTO) {
                            updateListenProgress(
                                queueItemId = oldItem.id,
                                position = 0L,
                                status = UserProgressStatus.COMPLETED,
                                completedAt = nowLocalDateTime(),
                            )
                        } else if (oldPositionMs > 0) {
                            updateListenProgress(
                                queueItemId = oldItem.id,
                                position = oldPositionMs,
                            )
                        }
                    }
                }

                override fun onPlaybackResumption() {
                    scope.launch {
                        val queuedItems = queueRepository.getQueuedItems()
                        if (queuedItems.isNotEmpty()) {
                            setItems(
                                queuedItems,
                                0,
                                queuedItems.getOrNull(0)?.lastPositionMs ?: 0L,
                            )
                        }
                    }
                }
            },
        )

        playerDelegate?.setTokenProvider(
            object : TokenProvider {
                override fun getTokens(): Pair<String, String>? = runBlocking { tokenManager.getTokens() }

                override fun refreshTokens(refreshToken: String) {
                    runBlocking {
                        tokenManager.refreshToken(refreshToken)
                    }
                }
            },
        )

        listenTrackingJob =
            scope.launch {
                val flow =
                    isPlaying.combine(itemId) { isPlaying, mediaItemId ->
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
                                playerDelegate?.let { player ->
                                    if (player.currentItem()?.id == it.second) {
                                        updateListenProgress(
                                            queueItemId = it.second!!,
                                            position = player.getCurrentPosition(),
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

    override fun isReady(): Boolean = true

    override fun isEmpty(): Boolean = playerDelegate?.getItemCount() == 0

    override fun addFirst(item: QueueItem) {
        playerDelegate?.let { playerDelegate ->
            playerDelegate.setPlayWhenReady(true)
            playerDelegate.add(index = 0, item = item)
            playerDelegate.seekTo(itemIndex = 0, positionMs = item.lastPositionMs)
        }
    }

    override fun moveToFirst(
        item: QueueItem,
        currentOrder: Int,
    ) {
        playerDelegate?.let { playerDelegate ->
            playerDelegate.setPlayWhenReady(true)
            playerDelegate.move(currentItem = item, currentIndex = currentOrder, newIndex = 0)
            playerDelegate.seekTo(itemIndex = 0, positionMs = item.lastPositionMs)
        }
    }

    override fun replaceFirst(item: QueueItem) {
        playerDelegate?.let { playerDelegate ->
            playerDelegate.replace(index = 0, item = item)
            playerDelegate.seekTo(positionMs = item.lastPositionMs)
        }
    }

    override fun setItems(
        items: List<QueueItem>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean?,
    ) {
        playerDelegate?.let { playerDelegate ->
            playerDelegate.setPlayWhenReady(playWhenReady ?: wasPlaying)
            playerDelegate.setItems(items, startIndex, startPositionMs)
        }
    }

    override fun playPause() {
        playerDelegate?.let { playerDelegate ->
            playerDelegate.playbackState().let { state ->
                if (state == PlaybackState.STATE_READY || state == PlaybackState.STATE_ENDED) {
                    if (playerDelegate.isPlaying()) {
                        playerDelegate.pause()
                    } else {
                        playerDelegate.setPlayWhenReady(true)
                        playerDelegate.play()
                    }
                } else if (state == PlaybackState.STATE_IDLE) {
                    playerDelegate.setPlayWhenReady(true)
                    playerDelegate.reload()
                }
            }
        }
    }

    override fun pause() {
        playerDelegate?.pause()
    }

    override fun getCurrentPositonMs(): Long {
        playerDelegate?.let { playerDelegate ->
            if (playerDelegate.currentItem() != null) {
                lastReturnedPositionMs = playerDelegate.getCurrentPosition()
            }
        }
        return lastReturnedPositionMs
    }

    override fun seekTo(positionMs: Long) {
        playerDelegate?.seekTo(positionMs)
    }

    override fun seek(forward: Boolean) {
        playerDelegate?.let {
            if (forward) {
                it.seekTo(min(it.getCurrentPosition() + 30_000, it.getDuration()))
            } else {
                it.seekTo(max(it.getCurrentPosition() - 10_000, 0))
            }
        }
    }

    override fun move(
        from: Int,
        to: Int,
    ) {
        playerDelegate?.move(null, from, to)
    }

    override fun remove(index: Int) {
        playerDelegate?.let { playerDelegate ->
            val currentItemIndex = playerDelegate.getCurrentItemIndex()
            val itemCount = playerDelegate.getItemCount()
            if (currentItemIndex == index) {
                playerDelegate.seekToNextItem()
            }
            if (itemCount == 1 || index != currentItemIndex) {
                playerDelegate.removeItem(index)
            }
        }
    }

    override fun clearQueue(completely: Boolean) {
        playerDelegate?.let { playerDelegate ->
            val start = if (completely) 0 else 1
            if (playerDelegate.getItemCount() > start) {
                playerDelegate.removeItems(start, playerDelegate.getItemCount() - 1)
            }
        }
    }

    override suspend fun stopService() {
        isStopped = true
        playerDelegate?.let { playerDelegate ->
            withContext(Dispatchers.Main) {
                wasPlaying = playerDelegate.playWhenReady() || playerDelegate.isPlaying()
                playerDelegate.setPlayWhenReady(false)
                playerDelegate.pause()
            }
            delay(500)
            listenTrackingJob?.cancel()
            listenTrackingJob = null
            withContext(Dispatchers.Main) {
                playerDelegate.release()
            }
        }
        playerDelegate = null
    }

    override fun startService() {
        isStopped = false
        playerDelegate = AVPlayerDelegate.factory.invoke()
        setup()
    }

    override fun destroy() {
        isStopped = true
        playerDelegate?.release()
        playerDelegate = null
    }
}

// MARK: - Factory Function
internal actual fun createPlayer(): PlayerController = IOSPlayerController()

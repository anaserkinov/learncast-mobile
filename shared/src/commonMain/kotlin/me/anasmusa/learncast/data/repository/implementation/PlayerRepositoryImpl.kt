package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.anasmusa.learncast.core.EVENT_SHOW_PLAYER
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.core.platform.isAndroid
import me.anasmusa.learncast.core.player.PlayerController
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import kotlin.math.max

internal class PlayerRepositoryImpl(
    private val queueRepository: QueueRepository,
    private val player: PlayerController,
) : PlayerRepository {
    private val scope = CoroutineScope(Dispatchers.Default)

    private var queueLoadJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentQueueItem =
        player.currentQueueItemId
            .flatMapLatest { if (it == null) flowOf(null) else queueRepository.observe(it) }
            .stateIn(scope, SharingStarted.Eagerly, null)
    override val playbackPositionMs = MutableStateFlow(0L)
    override val playbackState = player.playbackState
    override val events = Channel<Int>(Channel.BUFFERED)

    init {
        scope.launch {
            currentQueueItem
                .combine(playbackState) { queueItem, state ->
                    Pair(queueItem, state)
                }.collectLatest {
                    if (it.first != null) {
                        if (it.second == STATE_PLAYING) {
                            while (true) {
                                withContext(Dispatchers.Main) {
                                    playbackPositionMs.value = player.getCurrentPositonMs()
                                }
                                delay(1000)
                            }
                        } else {
                            var delayDuration = 1000L
                            while (true) {
                                withContext(Dispatchers.Main) {
                                    playbackPositionMs.value = player.getCurrentPositonMs()
                                }
                                delay(delayDuration)
                                delayDuration = max(0, delayDuration * 2)
                            }
                        }
                    }
                }
        }
    }

    override fun observeQueuedCount(): Flow<Int> = queueRepository.observeQueuedCount()

    override fun addToQueue(item: QueueItem) {
        currentQueueItem.value?.let {
            if (item.referenceId == it.referenceId &&
                item.referenceUuid == it.referenceUuid &&
                item.referenceType == it.referenceType
            ) {
                scope.launch {
                    events.send(EVENT_SHOW_PLAYER)
                }
                return
            }
        }
        queueLoadJob?.cancel()
        queueLoadJob =
            scope.launch {
                val triple = queueRepository.addToQueue(item) ?: return@launch
                playbackPositionMs.update { triple.first.lastPositionMs }
                withContext(Dispatchers.Main) {
                    val previousOrder = triple.second
                    if (previousOrder == -1) {
                        player.addFirst(triple.first)
                    } else {
                        player.moveToFirst(triple.first, previousOrder)
                    }
                    events.send(EVENT_SHOW_PLAYER)
                }
            }
    }

    override fun setToQueue(
        items: List<QueueItem>,
        playWhenReady: Boolean?,
    ) {
        playbackPositionMs.update {
            items.firstOrNull()?.lastPositionMs ?: 0L
        }
        scope.launch {
            withTimeout(5000) { while (!player.isReady()) delay(200) }
            withContext(Dispatchers.Main) {
                if (player.isEmpty() && items.isNotEmpty()) {
                    val currentPlaying = items.firstOrNull()
                    player.setItems(
                        items = items,
                        startIndex = 0,
                        startPositionMs = currentPlaying?.lastPositionMs ?: 0L,
                        playWhenReady = playWhenReady,
                    )
                }
            }
        }
    }

    override fun togglePlayback() {
        player.playPause()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        playbackPositionMs.value = player.getCurrentPositonMs()
    }

    override fun seek(forward: Boolean) {
        player.seek(forward)
        playbackPositionMs.value = player.getCurrentPositonMs()
    }

    override fun refreshCurrent() {
        currentQueueItem.value?.let { queueItem ->
            scope.launch {
                queueRepository.refreshQueueItem(queueItem.id, queueItem.referenceUuid)?.let { newQueueItem ->
                    withContext(Dispatchers.Main) {
                        player.replaceFirst(newQueueItem)
                    }
                }
            }
        }
    }

    override fun move(
        from: Int,
        to: Int,
    ) {
        player.move(from, to)
        scope.launch { queueRepository.move(from, to) }
    }

    override fun removeFromQueue(
        index: Int,
        id: Long,
    ) {
        player.remove(index)
        scope.launch { queueRepository.remove(id) }
    }

    override suspend fun clearQueue(completely: Boolean): Unit =
        withContext(Dispatchers.Main) {
            player.clearQueue(completely)
            scope.launch { queueRepository.clear(completely) }
        }

    override suspend fun stopService() =
        withContext(Dispatchers.Main) {
            player.stopService()
        }

    override suspend fun startService(playWhenReady: Boolean?) =
        withContext(Dispatchers.Main) {
            player.startService()
            val queuedItems = queueRepository.getQueuedItems()
            setToQueue(items = queuedItems, playWhenReady = playWhenReady)
        }

    override fun destroy() {
        if (isAndroid()) player.destroy()
    }
}

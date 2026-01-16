package me.anasmusa.learncast.ui.player

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.core.EVENT_SHOW_PLAYER
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.nowLocalDateTime
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import me.anasmusa.learncast.data.repository.abstraction.OutboxRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class PlayerState(
    val isLoading: Boolean = false,
    val currentPlaying: QueueItem? = null,
    val playbackState: Int = STATE_LOADING,
    val currentPositionMs: Long = 0L,
    val queuedCount: Int = 0,
    val snipCount: Int = -1,
) : me.anasmusa.learncast.ui.BaseState

sealed interface PlayerIntent : me.anasmusa.learncast.ui.BaseIntent {
    object TogglePlaybackState : PlayerIntent

    object Pause : PlayerIntent

    data class SeekTo(
        val value: Long,
    ) : PlayerIntent

    data class Seek(
        val forward: Boolean,
    ) : PlayerIntent

    data object Download : PlayerIntent

    data object RemoveDownload : PlayerIntent

    data object ToggleCompletedState : PlayerIntent

    data object ToggleFavourite : PlayerIntent

    data object DeleteSnip : PlayerIntent

    data object LoadSnipCount : PlayerIntent

    data object Refresh : PlayerIntent
}

sealed interface PlayerEvent : me.anasmusa.learncast.ui.BaseEvent {
    object ShowPlayer : PlayerEvent
}

class PlayerViewModel(
    private val outboxRepository: OutboxRepository,
    private val syncRepository: SyncRepository,
    private val playerRepository: PlayerRepository,
    private val downloadRepository: DownloadRepository,
    private val snipRepository: SnipRepository,
) : me.anasmusa.learncast.ui.BaseViewModel<PlayerState, PlayerIntent, PlayerEvent>() {
    override val state: StateFlow<PlayerState>
        field = MutableStateFlow(PlayerState())

    private var snipCountLoadJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                playerRepository.currentQueueItem.collectLatest { queueItem ->
                    if (state.value.currentPlaying?.id != queueItem?.id) {
                        snipCountLoadJob?.cancel()
                    }
                    state.update {
                        it.copy(
                            currentPlaying = queueItem,
                            snipCount =
                                if (it.currentPlaying?.id != queueItem?.id) {
                                    -1
                                } else {
                                    it.snipCount
                                },
                        )
                    }
                }
            }
            launch {
                playerRepository.playbackState.collectLatest { playbackState ->
                    state.update { it.copy(playbackState = playbackState) }
                }
            }
            launch {
                playerRepository.playbackPositionMs.collectLatest { currentPositionMs ->
                    state.update { it.copy(currentPositionMs = currentPositionMs) }
                }
            }
            launch {
                playerRepository.queuedCount.collectLatest { queuedCount ->
                    state.update { it.copy(queuedCount = queuedCount) }
                }
            }
            launch {
                playerRepository.events.receiveAsFlow().collect {
                    when (it) {
                        EVENT_SHOW_PLAYER ->
                            send(PlayerEvent.ShowPlayer)
                    }
                }
            }

            launch {
                syncRepository.sync(finishWhenDrained = false)
            }
        }
    }

    override fun handle(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.TogglePlaybackState -> playerRepository.togglePlayback()
            PlayerIntent.Pause -> playerRepository.pause()
            is PlayerIntent.SeekTo -> seekTo(intent.value)
            is PlayerIntent.Seek -> seek(intent.forward)
            is PlayerIntent.Download -> download()
            is PlayerIntent.RemoveDownload -> removeDownload()
            is PlayerIntent.ToggleCompletedState -> toggleCompletedState()
            is PlayerIntent.ToggleFavourite -> toggleFavourite()
            is PlayerIntent.LoadSnipCount -> loadSnipCount()
            is PlayerIntent.DeleteSnip -> deleteSnip()
            is PlayerIntent.Refresh -> refresh()
        }
    }

    private fun seekTo(value: Long) {
        playerRepository.seekTo(value)
    }

    private fun seek(forward: Boolean) {
        playerRepository.seek(forward)
    }

    private fun download() {
        state.update { it.copy(isLoading = true) }
        state.value.currentPlaying?.let {
            viewModelScope.launch {
                downloadRepository.download(
                    it.referenceId,
                    it.referenceUuid,
                    it.referenceType,
                    it.lessonId,
                    it.audioPath,
                    it.startMs,
                    it.endMs,
                )
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun removeDownload() {
        state.update { it.copy(isLoading = true) }
        state.value.currentPlaying?.let {
            viewModelScope.launch {
                downloadRepository.remove(
                    it.referenceId,
                    it.referenceUuid,
                    it.referenceType,
                )
                state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun toggleCompletedState() {
        viewModelScope.launch {
            state.value.currentPlaying?.let {
                outboxRepository.updateLessonProgress(
                    lessonId = it.referenceId,
                    startedAt = nowLocalDateTime(),
                    lastPositionMs = state.value.currentPositionMs.toDuration(DurationUnit.MILLISECONDS),
                    status =
                        if (it.status == UserProgressStatus.COMPLETED) {
                            UserProgressStatus.NOT_STARTED
                        } else {
                            UserProgressStatus.COMPLETED
                        },
                    completedAt =
                        if (it.status == UserProgressStatus.COMPLETED) {
                            null
                        } else {
                            nowLocalDateTime()
                        },
                )
                // User marked the lesson as completed → move to the next lesson
                if (it.status != UserProgressStatus.COMPLETED) {
                    playerRepository.removeFromQueue(
                        0,
                        it.id,
                    )
                }
            }
        }
    }

    private fun toggleFavourite() {
        viewModelScope.launch {
            state.value.currentPlaying?.let {
                outboxRepository.setFavourite(it.lessonId, !it.isFavourite)
            }
        }
    }

    private fun loadSnipCount() {
        if (snipCountLoadJob?.isActive == true) return
        state.value.currentPlaying?.let { queueItem ->
            snipCountLoadJob =
                viewModelScope.launch {
                    val snipCount = snipRepository.getSnipCount(queueItem.referenceId)
                    withContext(Dispatchers.Main) {
                        if (state.value.currentPlaying?.referenceId == queueItem.referenceId) {
                            state.update {
                                it.copy(snipCount = snipCount)
                            }
                        }
                    }
                }
        }
    }

    private fun deleteSnip() {
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            state.value.currentPlaying?.let {
                snipRepository.delete(it.referenceUuid)
                playerRepository.removeFromQueue(
                    0,
                    it.id,
                )
            }
            state.update { it.copy(isLoading = false) }
        }
    }

    private fun refresh() {
        state.value.currentPlaying?.let {
            if (it.referenceType == ReferenceType.LESSON) {
                snipCountLoadJob?.cancel()
                loadSnipCount()
            } else {
                playerRepository.refreshCurrent()
            }
        }
    }
}

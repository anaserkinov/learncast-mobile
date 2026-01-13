package me.anasmusa.learncast.ui.player.queue

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel
import kotlin.collections.toMutableList

data class QueueState(
    val isLoading: Boolean = true,
    val currentPlaying: QueueItem? = null,
    val playbackState: Int = STATE_LOADING,
    val currentPositionMs: Long = 0L,
    val queuedItems: MutableList<QueueItem> = mutableListOf()
): BaseState

sealed interface QueueIntent: BaseIntent{
    data class Move(val from: Int, val to: Int): QueueIntent
    data class Remove(val id: Long): QueueIntent
    object Clear: QueueIntent
    data class Play(val item: QueueItem) : QueueIntent
    object TogglePlayback: QueueIntent
}
sealed interface QueueEvent: BaseEvent

class QueueViewModel(
    private val queueRepository: QueueRepository,
    private val playerRepository: PlayerRepository
): BaseViewModel<QueueState, QueueIntent, QueueEvent>() {

    override val state: StateFlow<QueueState>
        field = MutableStateFlow(QueueState())

    init {
        viewModelScope.launch{
            launch {
                playerRepository.currentQueueItem.collectLatest { queueItem ->
                    state.update { it.copy(currentPlaying = queueItem) }
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
        }

        load()
    }

    override fun handle(intent: QueueIntent) {
        super.handle(intent)
        when(intent){
            is QueueIntent.Move -> {
                move(intent.from, intent.to)
            }
            is QueueIntent.Remove -> {
                remove(intent.id)
            }
            QueueIntent.Clear -> {
                clear()
            }
            is QueueIntent.Play -> {
                play(intent.item)
            }
            QueueIntent.TogglePlayback -> {
                playPause()
            }
        }
    }

    private fun load(){
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            state.update {
                it.copy(
                    isLoading = false,
                    queuedItems = ArrayList(queueRepository.getQueuedItems()).apply {
                        if (isNotEmpty()) removeFirst()
                    }
                )
            }
        }
    }

    private fun move(from: Int, to: Int){
        state.update {
            it.copy(
                queuedItems = it.queuedItems.toMutableList().apply{
                    add(to, removeAt(from))
                }
            )
        }
        playerRepository.move(from + 1, to + 1)
    }

    private fun remove(id: Long){
        if (id == state.value.currentPlaying?.id){
            if (!state.value.queuedItems.isEmpty()){
                state.update {
                    it.copy(
                        queuedItems = it.queuedItems.toMutableList().apply { removeFirst() }
                    )
                }
            }
            playerRepository.removeFromQueue(
                0,
                id
            )
        } else {
            val list = state.value.queuedItems.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            list.removeAt(index)
            state.update {
                it.copy(
                    queuedItems = list
                )
            }
            if (index != -1)
                playerRepository.removeFromQueue(
                    index + 1,
                    id
                )
        }
    }

    private fun clear(){
        state.update {
            it.copy(
                queuedItems = mutableListOf()
            )
        }
        playerRepository.clearQueue(false)
    }

    private fun play(item: QueueItem) {
        state.update {
            it.copy(
                queuedItems = it.queuedItems.toMutableList().apply{
                    add(0, it.currentPlaying!!)
                    remove(item)
                }
            )
        }
        playerRepository.addToQueue(item)
    }

    private fun playPause(){
        playerRepository.togglePlayback()
    }

}
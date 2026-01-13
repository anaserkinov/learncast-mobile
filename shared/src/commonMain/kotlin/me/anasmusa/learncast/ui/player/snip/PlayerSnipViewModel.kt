package me.anasmusa.learncast.ui.player.snip

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.data.mapper.toQueueItem
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class PlayerSnipState(
    val currentPlaying: QueueItem? = null,
    val playbackState: Int = STATE_LOADING,
    val currentPositionMs: Long = 0L,
    val snips: Flow<PagingData<Snip>> = emptyFlow()
): BaseState

sealed interface PlayerSnipIntent: BaseIntent{
    data class Load(val lessonId: Long): PlayerSnipIntent
    data class Play(val item: Snip) : PlayerSnipIntent
    object TogglePlayback: PlayerSnipIntent
}
sealed interface SnipEvent: BaseEvent

class PlayerSnipViewModel(
    private val snipRepository: SnipRepository,
    private val playerRepository: PlayerRepository
): BaseViewModel<PlayerSnipState, PlayerSnipIntent, SnipEvent>() {

    override val state: StateFlow<PlayerSnipState>
        field = MutableStateFlow(PlayerSnipState())

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
    }

    override fun handle(intent: PlayerSnipIntent) {
        super.handle(intent)
        when(intent){
            is PlayerSnipIntent.Load -> load(intent.lessonId)
            is PlayerSnipIntent.Play -> play(intent.item)
            PlayerSnipIntent.TogglePlayback -> togglePlayback()
        }
    }

    private fun load(lessonId: Long){
        viewModelScope.launch {
            state.update {
                it.copy(
                    snips = snipRepository.page(lessonId = lessonId)
                )
            }
        }
    }

    private fun play(item: Snip) {
        playerRepository.addToQueue(item.toQueueItem())
    }

    private fun togglePlayback(){
        playerRepository.togglePlayback()
    }

}
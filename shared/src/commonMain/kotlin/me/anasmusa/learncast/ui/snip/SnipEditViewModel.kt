package me.anasmusa.learncast.ui.snip

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.data.model.fold
import me.anasmusa.learncast.data.model.onSuccess
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import me.anasmusa.learncast.player.AudioPlayer
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.player.createAudioPlayer
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class SnipEditState(
    val playbackState: Int = STATE_LOADING,
    val isLoading: Boolean = false,
    val currentPositionMs: Long = 0L
): BaseState

sealed interface SnipEdiIntent: BaseIntent{
    data class Init(val clientSnipId: String, val audioPath: String, val startPosition: Long): SnipEdiIntent
    data class Start(val from: Int, val to: Int): SnipEdiIntent
    data object Stop: SnipEdiIntent
    data class Save(
        val clientSnipId: String,
        val queueItemId: Long,
        val start: Int,
        val end: Int,
        val note: String
    ): SnipEdiIntent
}

sealed interface SnipEditEvent: BaseEvent{
    data class ShowError(val message: String): SnipEditEvent
    data class OnSnipLoaded(val note: String?): SnipEditEvent
    data object Finish: SnipEditEvent
}

class SnipEditViewModel(
    private val snipRepository: SnipRepository
): BaseViewModel<SnipEditState, SnipEdiIntent, SnipEditEvent>(){

    private lateinit var audioPlayer: AudioPlayer

    override val state: StateFlow<SnipEditState>
        field = MutableStateFlow(SnipEditState())

    private var pauseAt = 0L

    override fun handle(intent: SnipEdiIntent) {
        super.handle(intent)
        when(intent){
            is SnipEdiIntent.Init -> init(intent)
            is SnipEdiIntent.Save -> save(intent)
            is SnipEdiIntent.Start -> start(intent)
            SnipEdiIntent.Stop -> stop()
        }
    }

    private fun init(intent: SnipEdiIntent.Init){
        audioPlayer = createAudioPlayer(intent.audioPath, intent.startPosition)
        viewModelScope.launch {
            if (!intent.clientSnipId.isEmpty())
                launch {
                    snipRepository.get(intent.clientSnipId).onSuccess {
                        send(SnipEditEvent.OnSnipLoaded(it.note))
                    }
                }
            launch {
                audioPlayer.playbackState.map { it == STATE_PLAYING }
                    .collectLatest {
                        do {
                            val pos = withContext(Dispatchers.Main){ audioPlayer.getCurrentPositonMs() }
                            state.update { it.copy(currentPositionMs = pos) }
                            if (pos > pauseAt) withContext(Dispatchers.Main){ audioPlayer.stop() }
                            delay(1000)
                        } while (it)
                    }
            }
            launch {
                audioPlayer.playbackState.collectLatest { playbackState ->
                    state.update {
                        it.copy(playbackState = playbackState)
                    }
                }
            }
        }
    }

    private fun start(intent: SnipEdiIntent.Start){
        pauseAt = intent.to * 1000L
        audioPlayer.start(intent.from * 1000L)
    }

    private fun stop(){
        audioPlayer.stop()
    }

    private fun save(intent: SnipEdiIntent.Save){
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            snipRepository.save(
                clientSnipId = intent.clientSnipId,
                queueItemId = intent.queueItemId,
                startMs = intent.start * 1000L,
                endMs = intent.end * 1000L,
                note = intent.note
            ).fold(
                onSuccess = {
                    send(SnipEditEvent.Finish)
                },
                onFailure = { message, _ ->
                    state.update { it.copy(isLoading = false) }
                    send(SnipEditEvent.ShowError(message))
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.destroy()
    }

}
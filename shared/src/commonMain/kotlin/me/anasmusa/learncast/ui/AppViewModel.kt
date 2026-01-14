package me.anasmusa.learncast.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.data.AppScope
import me.anasmusa.learncast.data.repository.abstraction.AppRepository
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import org.koin.mp.KoinPlatform

data class AppState(
    val isLoggedIn: Boolean? = null,
) : BaseState

sealed interface AppIntent : BaseIntent {
    object Load : AppIntent
}

sealed interface AppEvent : BaseEvent {
    object ShowHomeScreen : AppEvent

    object ShowLoginScreen : AppEvent
}

class AppViewModel(
    private val appRepository: AppRepository,
    private val authRepository: AuthRepository,
    private val queueRepository: QueueRepository,
    private val syncRepository: SyncRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<AppState, AppIntent, AppEvent>() {
    override val state: StateFlow<AppState>
        field = MutableStateFlow(AppState())

    private var appStateJob: Job? = null

    init {
        viewModelScope.launch {
            launch {
                syncRepository.sync(finishWhenDrained = false)
            }
        }
    }

    override fun handle(intent: AppIntent) {
        when (intent) {
            AppIntent.Load -> load()
        }
    }

    private fun load() {
        appStateJob?.cancel()
        appStateJob =
            viewModelScope.launch {
                authRepository.isLoggedIn().collect { isLoggedIn ->
                    if (state.value.isLoggedIn != isLoggedIn) {
                        if (state.value.isLoggedIn == null && isLoggedIn) {
                            loadQueuedItems()
                        } else if (state.value.isLoggedIn == true && !isLoggedIn) {
                            clearQueue()
                        }
                        state.update { it.copy(isLoggedIn = isLoggedIn) }
                        if (isLoggedIn) {
                            send(AppEvent.ShowHomeScreen)
                        } else {
                            send(AppEvent.ShowLoginScreen)
                        }
                    }
                }
            }
    }

    private fun loadQueuedItems() {
        viewModelScope.launch {
            val queuedItems = queueRepository.getQueuedItems()
            playerRepository.setToQueue(queuedItems, false)
        }
    }

    private fun clearQueue() {
        viewModelScope.launch {
            playerRepository.clearQueue(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerRepository.destroy()
        KoinPlatform.getKoin().getScopeOrNull(AppScope.ID)?.close()
    }
}

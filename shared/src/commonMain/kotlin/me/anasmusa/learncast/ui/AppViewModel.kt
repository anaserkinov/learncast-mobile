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
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import org.koin.mp.KoinPlatform

data class AppState(
    val isLoggedIn: Boolean? = null,
) : BaseState

sealed interface AppIntent : BaseIntent {
    object Load : AppIntent
}

sealed class AppEvent : BaseEvent {
    object ShowHomeScreen : AppEvent()

    object ShowLoginScreen : AppEvent()
}

class AppViewModel(
    private val appRepository: AppRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<AppState, AppIntent, AppEvent>() {
    final override val state: StateFlow<AppState>
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
                            playerRepository.startService(false)
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

    override fun onCleared() {
        super.onCleared()
        playerRepository.destroy()
        KoinPlatform.getKoin().getScopeOrNull(AppScope.ID)?.close()
    }
}

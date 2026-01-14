package me.anasmusa.learncast.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.data.model.User
import me.anasmusa.learncast.data.model.fold
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.UserRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class ProfileState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isQueueEmpty: Boolean = true,
) : BaseState

sealed interface ProfileIntent : BaseIntent {
    object Logout : ProfileIntent
}

sealed interface ProfileEvent : BaseEvent {
    data class ShowError(
        val message: String,
    ) : ProfileEvent
}

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<ProfileState, ProfileIntent, ProfileEvent>() {
    override val state: StateFlow<ProfileState>
        field = MutableStateFlow(ProfileState())

    init {
        viewModelScope.launch {
            launch {
                userRepository.getUser().fold(
                    onSuccess = { user ->
                        state.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                            )
                        }
                    },
                    onFailure = { message, _ ->
                        state.update { it.copy(isLoading = false) }
                        send(ProfileEvent.ShowError(message))
                    },
                )
            }
            launch {
                playerRepository.currentQueueItem.collect { queueItem ->
                    state.update {
                        it.copy(isQueueEmpty = queueItem == null)
                    }
                }
            }
        }
    }

    override fun handle(intent: ProfileIntent) {
        super.handle(intent)
        when (intent) {
            ProfileIntent.Logout -> logout()
        }
    }

    private fun logout() {
        state.update {
            it.copy(isLoading = true)
        }
        viewModelScope.launch {
            authRepository.logout()
            state.update {
                it.copy(isLoading = false)
            }
        }
    }
}

package me.anasmusa.learncast.ui.auth

import androidx.lifecycle.viewModelScope
import me.anasmusa.learncast.data.model.onFailure
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isLoading: Boolean = false
): BaseState

sealed interface LoginIntent: BaseIntent{
    data class LoginWithTelegram(val hash: String): LoginIntent
    object LoginWithGoogle: LoginIntent
}

sealed interface LoginEvent: BaseEvent{
    data class ShowError(val message: String): LoginEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository
): BaseViewModel<LoginState, LoginIntent, LoginEvent>() {

    override val state: StateFlow<LoginState>
        field = MutableStateFlow(LoginState())

    override fun handle(intent: LoginIntent) {
        when(intent){
            is LoginIntent.LoginWithTelegram -> loginWithTelegram(intent.hash)
            is LoginIntent.LoginWithGoogle -> loginWithGoogle()
        }
    }

    private fun loginWithTelegram(data: String){
        viewModelScope.launch {
            authRepository.loginWithTelegram(data).onFailure { message, tag ->
                state.update { it.copy(isLoading = false) }
                send(LoginEvent.ShowError(message))
            }
        }
    }

    private fun loginWithGoogle(){
        viewModelScope.launch {
            authRepository.loginWithGoogle().onFailure { message, tag ->
                state.update { it.copy(isLoading = false) }
                send(LoginEvent.ShowError(message))
            }
        }
    }
}
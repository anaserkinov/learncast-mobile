package me.anasmusa.learncast.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

interface BaseState

interface BaseIntent

interface BaseEvent

abstract class BaseViewModel<
    State : BaseState,
    Intent : BaseIntent,
    Event : BaseEvent,
> : ViewModel() {
    abstract val state: StateFlow<State>

    private val events = Channel<Event>(Channel.BUFFERED)

    open fun handle(intent: Intent) {
    }

    protected suspend fun send(event: Event) =
        withContext(Dispatchers.Main.immediate) {
            events.send(event)
        }

    fun subscribe(
        scope: CoroutineScope,
        onEvent: suspend (Event) -> Unit,
    ) {
        events
            .receiveAsFlow()
            .onEach(onEvent)
            .launchIn(scope + Dispatchers.Main.immediate)
    }
}

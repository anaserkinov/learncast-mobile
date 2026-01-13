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
import kotlin.jvm.JvmField

interface BaseState
interface BaseIntent
interface BaseEvent

abstract class BaseViewModel<
        State : BaseState,
        Intent : BaseIntent,
        Event : BaseEvent> : ViewModel() {

    abstract val state: StateFlow<State>

    private val _events = Channel<Event>(Channel.BUFFERED)

    open fun handle(intent: Intent) {

    }

    protected suspend fun send(event: Event) = withContext(Dispatchers.Main.immediate) {
        _events.send(event)
    }

    context(scope: CoroutineScope)
    fun subscribe(onEvent: suspend (Event) -> Unit) {
        _events.receiveAsFlow()
            .onEach(onEvent)
            .launchIn(scope + Dispatchers.Main.immediate)
    }

}
package me.anasmusa.learncast.ui.snip

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.anasmusa.learncast.data.mapper.toQueueItem
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.SnipRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class SnipListState(
    val searchQuery: String? = null,
    val inSearchMode: Boolean = false,
    val snips: Flow<PagingData<Snip>> = emptyFlow(),
) : BaseState

sealed interface SnipListIntent : BaseIntent {
    data class UpdateSearchQuery(
        val query: String?,
        val inSearchMode: Boolean,
    ) : SnipListIntent

    data class AddToQueue(
        val snip: Snip,
    ) : SnipListIntent
}

sealed interface SnipListEvent : BaseEvent

@OptIn(FlowPreview::class)
class SnipListViewModel(
    private val snipRepository: SnipRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<SnipListState, SnipListIntent, SnipListEvent>() {
    override val state: StateFlow<SnipListState>
        field = MutableStateFlow(SnipListState())

    init {
        state
            .map { it.searchQuery }
            .distinctUntilChanged()
            .debounce(500)
            .onEach { query ->
                state.update {
                    it.copy(
                        snips =
                            snipRepository.page(
                                search = query,
                                lessonId = null,
                                sort = null,
                                order = null,
                            ),
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun handle(intent: SnipListIntent) {
        when (intent) {
            is SnipListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query, intent.inSearchMode)
            is SnipListIntent.AddToQueue -> addToQueue(intent.snip)
        }
    }

    private fun updateSearchQuery(
        value: String?,
        inSearchMode: Boolean,
    ) {
        state.update { it.copy(searchQuery = value, inSearchMode = inSearchMode) }
    }

    private fun addToQueue(snip: Snip) {
        playerRepository.addToQueue(snip.toQueueItem())
    }
}

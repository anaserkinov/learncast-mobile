package me.anasmusa.learncast.ui.topic

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.repository.abstraction.TopicRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class TopicListState(
    val searchQuery: String? = null,
    val inSearchMode: Boolean = false,
    val topics: Flow<PagingData<Topic>> = emptyFlow(),
) : BaseState

sealed interface TopicListIntent : BaseIntent {
    data class UpdateSearchQuery(
        val query: String?,
        val inSearchMode: Boolean,
    ) : TopicListIntent
}

sealed interface TopicListEvent : BaseEvent

@OptIn(FlowPreview::class)
class TopicListViewModel(
    private val topicRepository: TopicRepository,
) : BaseViewModel<TopicListState, TopicListIntent, TopicListEvent>() {
    override val state: StateFlow<TopicListState>
        field = MutableStateFlow(TopicListState())

    init {
        viewModelScope.launch {
            state
                .map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { query ->
                    state.update {
                        it.copy(
                            topics = topicRepository.page(query, null),
                        )
                    }
                }
        }
    }

    override fun handle(intent: TopicListIntent) {
        super.handle(intent)
        when (intent) {
            is TopicListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query, intent.inSearchMode)
        }
    }

    private fun updateSearchQuery(
        value: String?,
        inSearchMode: Boolean,
    ) {
        state.update { it.copy(searchQuery = value, inSearchMode = inSearchMode) }
    }
}

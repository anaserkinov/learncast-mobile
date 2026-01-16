package me.anasmusa.learncast.ui

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
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository
import me.anasmusa.learncast.data.repository.abstraction.TopicRepository

data class SearchState(
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val lessons: Flow<PagingData<Lesson>> = emptyFlow(),
    val topics: Flow<PagingData<Topic>> = emptyFlow(),
) : BaseState

sealed interface SearchIntent : BaseIntent {
    data class Load(
        val authorId: Long,
        val topicId: Long?,
    ) : SearchIntent

    data class UpdateSearchQuery(
        val query: String,
    ) : SearchIntent

    data class SelectTab(
        val value: Int,
    ) : SearchIntent
}

sealed interface SearchEvent : BaseEvent

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val lessonRepository: LessonRepository,
    private val topicRepository: TopicRepository,
) : BaseViewModel<SearchState, SearchIntent, SearchEvent>() {
    final override val state: StateFlow<SearchState>
        field = MutableStateFlow(SearchState())

    override fun handle(intent: SearchIntent) {
        super.handle(intent)
        when (intent) {
            is SearchIntent.Load -> load(intent.authorId, intent.topicId)
            is SearchIntent.UpdateSearchQuery -> state.update { it.copy(searchQuery = intent.query) }
            is SearchIntent.SelectTab -> state.update { it.copy(selectedTab = intent.value) }
        }
    }

    private fun load(
        authorId: Long,
        topicId: Long?,
    ) {
        viewModelScope.launch {
            state
                .map { Pair(it.selectedTab, it.searchQuery) }
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { pair ->
                    state.update {
                        if (pair.first == 0) {
                            it.copy(
                                lessons =
                                    lessonRepository.page(
                                        search = pair.second,
                                        authorId = authorId,
                                        topicId = topicId,
                                    ),
                            )
                        } else {
                            it.copy(
                                topics =
                                    topicRepository.page(
                                        search = pair.second,
                                        authorId = authorId,
                                    ),
                            )
                        }
                    }
                }
        }
    }
}

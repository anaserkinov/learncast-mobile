package me.anasmusa.learncast.ui.home

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
import me.anasmusa.learncast.data.model.Filters
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class HomeState(
    val searchQuery: String? = null,
    val inSearchMode: Boolean = false,
    val selectedFilter: Filters = Filters.Latest,
    val lessons: Flow<PagingData<Lesson>> = emptyFlow()
) : BaseState

sealed interface HomeIntent : BaseIntent {
    data class UpdateSearchQuery(val query: String?, val inSearchMode: Boolean) : HomeIntent
    data class SelectFilter(val filter: Filters) : HomeIntent
    data class AddToQueue(val lesson: Lesson) : HomeIntent
}

sealed interface HomeEvent : BaseEvent {

}

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val lessonRepository: LessonRepository,
    private val playerRepository: PlayerRepository
) : BaseViewModel<HomeState, HomeIntent, HomeEvent>() {

    override val state: StateFlow<HomeState>
        field = MutableStateFlow(HomeState())

    init {
        state.map { it.searchQuery to it.selectedFilter }
            .distinctUntilChanged()
            .debounce(500)
            .onEach { (query, filter) ->
                var status: UserProgressStatus? = null
                var isDownloaded: Boolean? = null
                var sort: QuerySort? = null
                var order: QueryOrder? = null
                var isFavourite: Boolean? = null
                when (filter) {
                    Filters.Latest -> {
                        sort = QuerySort.CREATED_AT
                        order = QueryOrder.DESC
                    }

                    Filters.InProgress -> {
                        status = UserProgressStatus.IN_PROGRESS
                    }

                    Filters.Downloads -> {
                        isDownloaded = true
                    }

                    Filters.MostSnipped -> {
                        sort = QuerySort.SNIP_COUNT
                        order = QueryOrder.DESC
                    }

                    Filters.Favourite -> {
                        isFavourite = true
                    }
                }
                state.update {
                    it.copy(
                        lessons = lessonRepository.page(
                            search = query,
                            authorId = null,
                            topicId = null,
                            isFavourite = isFavourite,
                            status = status,
                            isDownloaded = isDownloaded,
                            sort = sort,
                            order = order
                        )
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun handle(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.UpdateSearchQuery -> updateSearchQuery(intent.query, intent.inSearchMode)
            is HomeIntent.SelectFilter -> selectFilter(intent.filter)
            is HomeIntent.AddToQueue -> addToQueue(intent.lesson)
        }
    }

    private fun updateSearchQuery(value: String?, inSearchMode: Boolean){
        state.update { it.copy(searchQuery = value, inSearchMode = inSearchMode) }
    }

    private fun selectFilter(filter: Filters) {
        state.update {
            it.copy(selectedFilter = filter)
        }
    }

    private fun addToQueue(lesson: Lesson) {
        playerRepository.addToQueue(lesson.toQueueItem())
    }

}
package me.anasmusa.learncast.ui.author

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.anasmusa.learncast.data.mapper.toQueueItem
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository
import me.anasmusa.learncast.data.repository.abstraction.TopicRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class AuthorState(
    val selectedTabIndex: Int = 0,
    val lessons: Flow<PagingData<Lesson>> = emptyFlow(),
    val topics: Flow<PagingData<Topic>> = emptyFlow()
) : BaseState

sealed interface AuthorIntent : BaseIntent {
    data class SelectTab(val index: Int): AuthorIntent
    data class LoadLessons(val authorId: Long) : AuthorIntent
    data class LoadTopics(val authorId: Long) : AuthorIntent
    data class AddToQueue(val lesson: Lesson) : AuthorIntent
}

sealed interface AuthorEvent : BaseEvent {

}

@OptIn(FlowPreview::class)
class AuthorViewModel(
    private val lessonRepository: LessonRepository,
    private val topicRepository: TopicRepository,
    private val playerRepository: PlayerRepository
) : BaseViewModel<AuthorState, AuthorIntent, AuthorEvent>() {

    override val state: StateFlow<AuthorState>
        field = MutableStateFlow(AuthorState())


    override fun handle(intent: AuthorIntent) {
        super.handle(intent)
        when (intent) {
            is AuthorIntent.SelectTab -> selectTab(intent.index)
            is AuthorIntent.LoadLessons -> loadLessons(intent.authorId)
            is AuthorIntent.LoadTopics -> loadTopics(intent.authorId)
            is AuthorIntent.AddToQueue -> addToQueue(intent.lesson)
        }
    }

    private fun selectTab(index: Int){
        viewModelScope.launch {
            state.update {
                it.copy(selectedTabIndex = index)
            }
        }
    }

    private fun loadLessons(authorId: Long) {
        if (state.value.topics === emptyFlow<Lesson>())
            viewModelScope.launch {
                state.update {
                    it.copy(
                        lessons = lessonRepository.page(
                            authorId = authorId
                        )
                    )
                }
            }
    }

    private fun loadTopics(authorId: Long) {
        if (state.value.topics === emptyFlow<Topic>())
            viewModelScope.launch {
                state.update {
                    it.copy(
                        topics = topicRepository.page(
                            authorId = authorId
                        )
                    )
                }
            }
    }

    private fun addToQueue(lesson: Lesson) {
        playerRepository.addToQueue(lesson.toQueueItem())
    }

}
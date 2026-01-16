package me.anasmusa.learncast.ui.topic

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
import me.anasmusa.learncast.data.repository.abstraction.LessonRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.QueueRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class TopicState(
    val isLoading: Boolean = false,
    val lessons: Flow<PagingData<Lesson>> = emptyFlow(),
) : BaseState

sealed interface TopicIntent : BaseIntent {
    data class Load(
        val topicId: Long,
        val authorId: Long,
    ) : TopicIntent

    data class PlayAll(
        val topicId: Long,
        val authorId: Long,
    ) : TopicIntent

    data class AddToQueue(
        val lesson: Lesson,
    ) : TopicIntent
}

sealed interface TopicEvent : BaseEvent

@OptIn(FlowPreview::class)
class TopicViewModel(
    private val lessonRepository: LessonRepository,
    private val queueRepository: QueueRepository,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<TopicState, TopicIntent, TopicEvent>() {
    final override val state: StateFlow<TopicState>
        field = MutableStateFlow(TopicState())

    override fun handle(intent: TopicIntent) {
        super.handle(intent)
        when (intent) {
            is TopicIntent.Load -> load(intent.topicId, intent.authorId)
            is TopicIntent.PlayAll -> playAll(intent.topicId, intent.authorId)
            is TopicIntent.AddToQueue -> addToQueue(intent.lesson)
        }
    }

    private fun load(
        topicId: Long,
        authorId: Long,
    ) {
        viewModelScope.launch {
            state.update {
                it.copy(
                    lessons =
                        lessonRepository.page(
                            authorId = authorId,
                            topicId = topicId,
                        ),
                )
            }
        }
    }

    private fun playAll(
        topicId: Long,
        authorId: Long,
    ) {
        state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val queueItems = queueRepository.addToQueue(topicId, authorId)
            if (queueItems.isNotEmpty()) {
                playerRepository.setToQueue(queueItems, true)
            }
            state.update { it.copy(isLoading = true) }
        }
    }

    private fun addToQueue(lesson: Lesson) {
        playerRepository.addToQueue(lesson.toQueueItem())
    }
}

package me.anasmusa.learncast.ui.author

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
import me.anasmusa.learncast.data.model.Author
import me.anasmusa.learncast.data.repository.abstraction.AuthorRepository
import me.anasmusa.learncast.ui.BaseEvent
import me.anasmusa.learncast.ui.BaseIntent
import me.anasmusa.learncast.ui.BaseState
import me.anasmusa.learncast.ui.BaseViewModel

data class AuthorListState(
    val searchQuery: String? = null,
    val inSearchMode: Boolean = false,
    val authors: Flow<PagingData<Author>> = emptyFlow()
): BaseState

sealed interface AuthorListIntent: BaseIntent{
    data class UpdateSearchQuery(val query: String?, val inSearchMode: Boolean): AuthorListIntent
}

sealed interface AuthorListEvent: BaseEvent {

}

@OptIn(FlowPreview::class)
class AuthorListViewModel(
    private val authorRepository: AuthorRepository
): BaseViewModel<AuthorListState, AuthorListIntent, AuthorListEvent>() {

    override val state: StateFlow<AuthorListState>
        field = MutableStateFlow(AuthorListState())

    init {
        viewModelScope.launch {
            state
                .map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { query ->
                    state.update {
                        it.copy(
                            authors = authorRepository.page(query)
                        )
                    }
                }
        }
    }

    override fun handle(intent: AuthorListIntent) {
        super.handle(intent)
        when(intent){
            is AuthorListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query, intent.inSearchMode)
        }
    }


    private fun updateSearchQuery(value: String?, inSearchMode: Boolean){
        state.update { it.copy(searchQuery = value, inSearchMode = inSearchMode) }
    }

}
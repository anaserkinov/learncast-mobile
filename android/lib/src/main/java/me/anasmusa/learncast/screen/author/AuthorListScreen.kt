package me.anasmusa.learncast.screen.author

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.SearchButton
import me.anasmusa.learncast.component.cell.AuthorCell
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.backgroundBrush
import me.anasmusa.learncast.data.model.Author
import me.anasmusa.learncast.nav.Screen
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.ui.author.AuthorListIntent
import me.anasmusa.learncast.ui.author.AuthorListViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TopicListScreenPreview(){
    AppTheme {
        _AuthorListScreen(
            onBackClicked = {},
            isRefreshing = false,
            onRefresh = {},
            searchQuery = null,
            onQueryChanged = {},
            authorCount = 1,
            authorKey = { 1 },
            getAuthor = {
                Author(
                    1,
                    "Author",
                    null,
                    20
                )
            },
            appendLoading = true,
            onAuthorClicked = {}
        )
    }
}

@Composable
fun AuthorListScreen() {

    val env = LocalAppEnvironment.current
    val viewModel = koinViewModel<AuthorListViewModel>()
    val state by viewModel.state.collectAsState()
    val pagingState = state.authors.collectAsLazyPagingItems()

    _AuthorListScreen(
        onBackClicked = {
            env.popBack()
        },
        isRefreshing = pagingState.loadState.refresh is LoadState.Loading,
        onRefresh = {
            pagingState.refresh()
        },
        searchQuery = if (state.inSearchMode) state.searchQuery?:"" else null,
        onQueryChanged = {
            viewModel.handle(
                AuthorListIntent.UpdateSearchQuery(
                    query = if (it == "") null else it,
                    inSearchMode = it != null
                )
            )
        },
        authorCount = pagingState.itemCount,
        authorKey = pagingState.itemKey { it.id },
        getAuthor = { pagingState[it] },
        appendLoading = pagingState.loadState.append is LoadState.Loading,
        onAuthorClicked = {
            env.navigate(Screen.Author(it))
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun  _AuthorListScreen(
    onBackClicked: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    searchQuery: String?,
    onQueryChanged: (value: String?) -> Unit,
    authorCount: Int,
    authorKey: (index: Int) -> Any,
    getAuthor: (index: Int) -> Author?,
    appendLoading: Boolean,
    onAuthorClicked: (author: Author) -> Unit
){
    Scaffold(
        modifier = Modifier
            .background(backgroundBrush()),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        modifier = Modifier,
                        text = Strings.authors.string(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClicked
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) {
        PullToRefreshBox (
            modifier = Modifier
                .padding(it),
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp
                    )
            ) {
                stickyHeader {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchButton(
                            searchQuery = searchQuery,
                            onQueryChanged = onQueryChanged
                        )
                    }
                }

                items(
                    authorCount,
//                    key = authorKey
                ) { index ->
                    val author = getAuthor(index)
                    if (author != null)
                        AuthorCell(
                            author = author,
                            onClick = {
                                onAuthorClicked(author)
                            }
                        )
                }

                if (appendLoading)
                    item {
                        LoadingIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
            }
        }
    }

}
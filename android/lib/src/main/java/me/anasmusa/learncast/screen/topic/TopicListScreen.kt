package me.anasmusa.learncast.screen.topic

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
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.SearchButton
import me.anasmusa.learncast.component.cell.TopicCell
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.backgroundBrush
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.model.getSampleTopic
import me.anasmusa.learncast.nav.Screen
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.ui.topic.TopicListIntent
import me.anasmusa.learncast.ui.topic.TopicListState
import me.anasmusa.learncast.ui.topic.TopicListViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TopicListScreenPreview(){
    AppTheme {
        _TopicListScreen(
            state = TopicListState(
                null,
                false,
                flowOf(PagingData.from(
                    listOf(getSampleTopic())
                ))
            ),
            onBackClicked = {},
            onQueryChanged = {},
            onTopicClicked = {}
        )
    }
}

@Composable
fun TopicListScreen() {

    val env = LocalAppEnvironment.current
    val viewModel = koinViewModel<TopicListViewModel>()
    val state by viewModel.state.collectAsState()

    _TopicListScreen(
        state = state,
        onBackClicked = {
            env.popBack()
        },
        onQueryChanged = {
            viewModel.handle(
                TopicListIntent.UpdateSearchQuery(
                    query = if (it == "") null else it,
                    inSearchMode = it != null
                )
            )
        },
        onTopicClicked = {
            env.navigate(Screen.Topic(it))
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun  _TopicListScreen(
    state: TopicListState,
    onBackClicked: () -> Unit,
    onQueryChanged: (value: String?) -> Unit,
    onTopicClicked: (topic: Topic) -> Unit
){
    val pagingState = state.topics.collectAsLazyPagingItems()

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
                        text = Strings.topics.string(),
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
            isRefreshing = pagingState.loadState.refresh is LoadState.Loading,
            onRefresh = { pagingState.refresh() }
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
                            searchQuery = if (state.inSearchMode) state.searchQuery?:"" else null,
                            onQueryChanged = onQueryChanged
                        )
                    }
                }

                items(
                    pagingState.itemCount,
//                    key = pagingState.itemKey { it.id }
                ) { index ->
                    val topic = pagingState[index]
                    if (topic != null)
                        TopicCell(
                            topic = topic,
                            onClick = {
                                onTopicClicked(topic)
                            }
                        )
                }

                if (pagingState.loadState.append is LoadState.Loading)
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
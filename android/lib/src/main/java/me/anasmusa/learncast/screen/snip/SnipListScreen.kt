package me.anasmusa.learncast.screen.snip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.SearchButton
import me.anasmusa.learncast.component.cell.SnipCell
import me.anasmusa.learncast.core.BOTTOM_PADDING
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.model.getSampleSnip
import me.anasmusa.learncast.string
import me.anasmusa.learncast.ui.snip.SnipListIntent
import me.anasmusa.learncast.ui.snip.SnipListState
import me.anasmusa.learncast.ui.snip.SnipListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
private fun SnipListScreenPreview() {
    AppTheme {
        _SnipListScreen(
            state =
                SnipListState(
                    searchQuery = "fdg",
                    inSearchMode = true,
                    snips =
                        flowOf(
                            PagingData.from(
                                listOf(getSampleSnip()),
                            ),
                        ),
                ),
            hazeState = rememberHazeState(),
            onQueryChanged = {},
            onSnipClicked = {},
        )
    }
}

@Composable
fun SnipListScreen() {
    val env = LocalAppEnvironment.current
    val viewModel = koinViewModel<SnipListViewModel>()
    val state by viewModel.state.collectAsState()

    _SnipListScreen(
        state = state,
        hazeState = env.hazeState,
        onQueryChanged = {
            viewModel.handle(
                SnipListIntent.UpdateSearchQuery(
                    query = if (it == "") null else it,
                    inSearchMode = it != null,
                ),
            )
        },
        onSnipClicked = {
            viewModel.handle(SnipListIntent.AddToQueue(it))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _SnipListScreen(
    state: SnipListState,
    hazeState: HazeState,
    onQueryChanged: (value: String?) -> Unit,
    onSnipClicked: (snip: Snip) -> Unit,
) {
    val pagingState = state.snips.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(
                    Brush.verticalGradient(
                        colors = LocalAppEnvironment.current.backgroundColors,
                        endY = with(LocalDensity.current) { 100.dp.toPx() },
                    ),
                ),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                scrollBehavior = scrollBehavior,
                title = {
                    Column(
                        modifier =
                            Modifier
                                .padding(top = 16.dp),
                    ) {
                        Text(
                            text = Strings.SNIPS.string(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier =
                                Modifier
                                    .padding(top = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SearchButton(
                                searchQuery = if (state.inSearchMode) state.searchQuery ?: "" else null,
                                onQueryChanged = onQueryChanged,
                            )
                        }
                    }
                },
            )
        },
    ) {
        PullToRefreshBox(
            modifier =
                Modifier
                    .padding(it),
            isRefreshing = pagingState.loadState.refresh is LoadState.Loading,
            onRefresh = { pagingState.refresh() },
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .hazeSource(hazeState)
                        .fillMaxSize()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                        ),
                contentPadding =
                    PaddingValues(
                        bottom = BOTTOM_PADDING.dp,
                    ),
            ) {
                items(
                    pagingState.itemCount,
//                    key = pagingState.itemKey { it.id }
                ) { index ->
                    val snip = pagingState.get(index)
                    if (snip != null) {
                        SnipCell(
                            snip = snip,
                            onClick = {
                                onSnipClicked(snip)
                            },
                        )
                    }
                }

                if (pagingState.loadState.append is LoadState.Loading) {
                    item {
                        LoadingIndicator(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentWidth(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

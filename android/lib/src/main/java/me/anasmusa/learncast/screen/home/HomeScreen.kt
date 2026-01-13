package me.anasmusa.learncast.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.simplestarts.app.ui.theme.icons.PersonIcon
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.PrimaryButton
import me.anasmusa.learncast.component.SearchButton
import me.anasmusa.learncast.component.cell.LessonCell
import me.anasmusa.learncast.core.BOTTOM_PADDING
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.backgroundBrush
import me.anasmusa.learncast.data.model.Filters
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.getSampleLesson
import me.anasmusa.learncast.nav.Screen
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.GridIcon
import me.anasmusa.learncast.ui.home.HomeIntent
import me.anasmusa.learncast.ui.home.HomeState
import me.anasmusa.learncast.ui.home.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun HomeScreenPreview() {
    AppTheme {
        _HomeScreen(
            HomeState(
                lessons = flowOf(
                    PagingData.from(
                        listOf(getSampleLesson())
                    )
                )
            ),
            hazeState = rememberHazeState(),
            navigate = {},
            onQueryChanged = {},
            onFilterSelected = {},
            onLessonClicked = {}
        )
    }
}

@Composable
fun HomeScreen() {

    val env = LocalAppEnvironment.current
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.state.collectAsState()

    _HomeScreen(
        state = state,
        hazeState = env.hazeState,
        navigate = {
            env.navigate(it)
        },
        onQueryChanged = {
            viewModel.handle(
                HomeIntent.UpdateSearchQuery(
                    query = if (it == "") null else it,
                    inSearchMode = it != null
                )
            )
        },
        onFilterSelected = {
            viewModel.handle(HomeIntent.SelectFilter(it))
        },
        onLessonClicked = {
            viewModel.handle(HomeIntent.AddToQueue(it))
        }
    )

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _HomeScreen(
    state: HomeState,
    hazeState: HazeState,
    navigate: (screen: Screen) -> Unit,
    onQueryChanged: (value: String?) -> Unit,
    onFilterSelected: (filter: Filters) -> Unit,
    onLessonClicked: (lesson: Lesson) -> Unit
) {
    val pagingState = state.lessons.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(backgroundBrush()),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                scrollBehavior = scrollBehavior,
                title = {
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            text = Strings.home.string(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PrimaryButton(
                                modifier = Modifier.weight(1f),
                                icon = PersonIcon,
                                title = Strings.authors
                            ) {
                                navigate(Screen.AuthorList)
                            }
                            PrimaryButton(
                                modifier = Modifier.weight(1f),
                                icon = GridIcon,
                                title = Strings.topics
                            ) {
                                navigate(Screen.TopicList)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(top = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SearchButton(
                                searchQuery = if (state.inSearchMode) state.searchQuery?:"" else null,
                                onQueryChanged = onQueryChanged
                            )
                        }

                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Filters.entries.forEach {
                                FilterChip(
                                    selected = it == state.selectedFilter,
                                    onClick = {
                                        onFilterSelected(it)
                                    },
                                    label = {
                                        Text(
                                            text = it.title.string()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .padding(it),
            isRefreshing = pagingState.loadState.refresh is LoadState.Loading,
            onRefresh = { pagingState.refresh() }
        ) {
            LazyColumn(
                modifier = Modifier
                    .hazeSource(hazeState)
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp
                    ),
                contentPadding = PaddingValues(
                    bottom = BOTTOM_PADDING.dp
                )
            ) {
                items(
                    pagingState.itemCount,
//                    key = pagingState.itemKey { it.id }
                ) { index ->
                    val lesson = pagingState[index]
                    if (lesson != null)
                        LessonCell(
                            lesson = lesson,
                            onClick = {
                                onLessonClicked(lesson)
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
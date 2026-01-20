package me.anasmusa.learncast.lib.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import dev.chrisbanes.haze.hazeSource
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.SearchInput
import me.anasmusa.learncast.lib.component.cell.LessonCell
import me.anasmusa.learncast.lib.component.cell.TopicCell
import me.anasmusa.learncast.lib.core.BOTTOM_PADDING
import me.anasmusa.learncast.lib.core.LocalAppEnvironment
import me.anasmusa.learncast.lib.core.backgroundBrush
import me.anasmusa.learncast.lib.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.ui.SearchIntent
import me.anasmusa.learncast.ui.SearchState
import me.anasmusa.learncast.ui.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@Preview
@Composable
fun SearchScreenPreview() {
    AppTheme {
        _SearchScreen(
            state = SearchState(),
            withTab = true,
            onQueryChanged = {},
            onTabSelected = {},
            openTopic = {},
            addToQueue = {},
        )
    }
}

@Composable
fun SearchScreen(
    authorId: Long,
    topicId: Long?,
    selectedTab: Int,
) {
    val viewModel = koinViewModel<SearchViewModel>()
    LaunchedEffect(viewModel) {
        if (selectedTab != 0) {
            viewModel.handle(SearchIntent.SelectTab(selectedTab))
        }
        viewModel.handle(SearchIntent.Load(authorId, topicId))
    }

    _SearchScreen(
        state = viewModel.state.collectAsState().value,
        withTab = topicId == null,
        onQueryChanged = {
            viewModel.handle(SearchIntent.UpdateSearchQuery(it))
        },
        onTabSelected = {
            viewModel.handle(SearchIntent.SelectTab(it))
        },
        openTopic = {
        },
        addToQueue = {
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun _SearchScreen(
    state: SearchState,
    withTab: Boolean,
    onQueryChanged: (value: String) -> Unit,
    onTabSelected: (index: Int) -> Unit,
    openTopic: (Topic) -> Unit,
    addToQueue: (Lesson) -> Unit,
) {
    val env = LocalAppEnvironment.current

    val lessons = state.lessons.collectAsLazyPagingItems()
    val topics = state.topics.collectAsLazyPagingItems()

    val currentPagingItems =
        if (state.selectedTab == 0) {
            lessons
        } else {
            topics
        }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier =
            Modifier.Companion
                .background(backgroundBrush()),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    SearchInput(
                        modifier = Modifier,
                        text = state.searchQuery,
                        focusRequester = focusRequester,
                        onTextChange = onQueryChanged,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { env.popBack() },
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) {
        PullToRefreshBox(
            modifier =
                Modifier
                    .padding(it),
            isRefreshing = currentPagingItems.loadState.refresh is LoadState.Loading,
            onRefresh = { currentPagingItems.refresh() },
        ) {
            if (withTab) {
                SecondaryTabRow(
                    selectedTabIndex = state.selectedTab,
                    containerColor = Color.Transparent,
                ) {
                    Tab(
                        selected = state.selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = {
                            Text(
                                text = Strings.LESSONS.string(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                    Tab(
                        selected = state.selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = {
                            Text(
                                text = Strings.TOPICS.string(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .hazeSource(LocalAppEnvironment.current.hazeState)
                        .fillMaxSize()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = if (withTab) 50.dp else 0.dp,
                        ),
                contentPadding =
                    PaddingValues(
                        bottom = BOTTOM_PADDING.dp,
                    ),
            ) {
                items(
                    currentPagingItems.itemCount,
//                    key = currentPagingItems.itemKey { it.id }
                ) { index ->
                    val any = currentPagingItems[index]
                    if (any != null) {
                        if (state.selectedTab == 0) {
                            LessonCell(
                                lesson = any as Lesson,
                                onClick = {
                                    addToQueue(any)
                                },
                            )
                        } else {
                            TopicCell(
                                topic = any as Topic,
                                onClick = {
                                    openTopic(any)
                                },
                            )
                        }
                    }
                }

                if (currentPagingItems.loadState.append is LoadState.Loading) {
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

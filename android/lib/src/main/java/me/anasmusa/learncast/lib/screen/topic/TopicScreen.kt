package me.anasmusa.learncast.lib.screen.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.model.getSampleLesson
import me.anasmusa.learncast.data.model.getSampleTopic
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.Loader
import me.anasmusa.learncast.lib.component.PrimaryButton
import me.anasmusa.learncast.lib.component.cell.LessonCell
import me.anasmusa.learncast.lib.core.BOTTOM_PADDING
import me.anasmusa.learncast.lib.core.LocalAppEnvironment
import me.anasmusa.learncast.lib.core.backgroundBrush
import me.anasmusa.learncast.lib.nav.Screen
import me.anasmusa.learncast.lib.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.lib.theme.icon.SearchIcon
import me.anasmusa.learncast.ui.topic.TopicIntent
import me.anasmusa.learncast.ui.topic.TopicState
import me.anasmusa.learncast.ui.topic.TopicViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
private fun TopicScreenPreview() {
    AppTheme {
        _TopicScreen(
            state =
                TopicState(
                    lessons =
                        flowOf(
                            PagingData.from(
                                listOf(getSampleLesson()),
                            ),
                        ),
                ),
            topic = getSampleTopic(),
            playAll = {},
            onLessonClicked = {},
        )
    }
}

@Composable
fun TopicScreen(topic: Topic) {
    val viewModel = koinViewModel<TopicViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.handle(TopicIntent.Load(topic.topicId, topic.authorId))
    }

    _TopicScreen(
        state = state,
        topic = topic,
        playAll = {
            viewModel.handle(TopicIntent.PlayAll(topic.topicId, topic.authorId))
        },
        onLessonClicked = {
            viewModel.handle(TopicIntent.AddToQueue(it))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _TopicScreen(
    state: TopicState,
    topic: Topic,
    playAll: () -> Unit,
    onLessonClicked: (lesson: Lesson) -> Unit,
) {
    val env = LocalAppEnvironment.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pagingState = state.lessons.collectAsLazyPagingItems()

    Scaffold(
        modifier =
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .background(backgroundBrush()),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
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
                title = {
                    Text(
                        text = topic.title,
                        maxLines = 1,
                    )
                },
                subtitle = {
                    Text(
                        text = topic.authorName,
                        maxLines = 1,
                    )
                },
            )
        },
        containerColor = Color.Transparent,
    ) {
        PullToRefreshBox(
            modifier =
                Modifier
                    .padding(it),
            isRefreshing = pagingState.loadState.refresh is LoadState.Loading,
            onRefresh = { pagingState.refresh() },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton(
                    modifier =
                        Modifier
                            .wrapContentSize(),
                    title = Strings.PLAY_ALL,
                    icon = null,
                    padding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
                    onClick = {
                        playAll()
                    },
                )

                Spacer(
                    modifier =
                        Modifier
                            .width(24.dp),
                )

                IconButton(
                    onClick = {
                        env.navigate(Screen.Search(topic.authorId, topic.topicId))
                    },
                ) {
                    Icon(
                        imageVector = SearchIcon,
                        contentDescription = null,
                    )
                }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .hazeSource(env.hazeState)
                        .fillMaxSize()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 50.dp,
                        ),
                contentPadding =
                    PaddingValues(
                        bottom = BOTTOM_PADDING.dp,
                    ),
            ) {
                if (topic.description != null) {
                    item {
                        Text(
                            modifier = Modifier,
                            text = topic.description!!,
                        )
                    }
                }

                items(
                    pagingState.itemCount,
                    key = pagingState.itemKey { it.id },
                ) { index ->
                    val lesson = pagingState[index]
                    if (lesson != null) {
                        LessonCell(
                            lesson = lesson,
                            onClick = {
                                onLessonClicked(lesson)
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

    if (state.isLoading) {
        Loader()
    }
}

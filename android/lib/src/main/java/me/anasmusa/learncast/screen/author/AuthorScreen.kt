package me.anasmusa.learncast.screen.author

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.cell.LessonCell
import me.anasmusa.learncast.component.cell.TopicCell
import me.anasmusa.learncast.core.BOTTOM_PADDING
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.backgroundBrush
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.Author
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.model.getSampleAuthor
import me.anasmusa.learncast.data.model.getSampleLesson
import me.anasmusa.learncast.nav.Screen
import me.anasmusa.learncast.quantityString
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.ArrowBackIcon
import me.anasmusa.learncast.theme.icon.SearchIcon
import me.anasmusa.learncast.ui.author.AuthorIntent
import me.anasmusa.learncast.ui.author.AuthorViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

@Composable
@Preview
private fun AuthorScreenPreview() {
    AppTheme {
        _AuthorScreen(
            author = getSampleAuthor(),
            isRefreshing = false,
            onRefresh = {},
            selectedTabIndex = 0,
            onTabSelected = {},
            itemCount = 1,
            itemKey = { 1 },
            getLesson = {
                getSampleLesson()
            },
            getTopic = {
                null
            },
            appendLoading = true,
            onItemClicked = {}
        )
    }
}

@Composable
fun AuthorScreen(author: Author) {

    val env = LocalAppEnvironment.current
    val viewModel = koinViewModel<AuthorViewModel>()
    val state by viewModel.state.collectAsState()
    val lessonPagingState = state.lessons.collectAsLazyPagingItems()
    val topicPagingState = state.topics.collectAsLazyPagingItems()

    LaunchedEffect(viewModel) {
        viewModel.handle(AuthorIntent.LoadLessons(author.id))
    }

    _AuthorScreen(
        author = author,
        isRefreshing = lessonPagingState.loadState.refresh is LoadState.Loading,
        onRefresh = {
            if (state.selectedTabIndex == 0) lessonPagingState.refresh()
            else topicPagingState.refresh()
        },
        selectedTabIndex = state.selectedTabIndex,
        onTabSelected = {
            if (it == 1)
                viewModel.handle(AuthorIntent.LoadTopics(author.id))
            viewModel.handle(AuthorIntent.SelectTab(it))
        },
        itemCount = (if (state.selectedTabIndex == 0) lessonPagingState else topicPagingState).itemCount,
        itemKey = if (state.selectedTabIndex == 0) lessonPagingState.itemKey { it.id } else topicPagingState.itemKey { it.id },
        getLesson = { lessonPagingState[it] },
        getTopic = { topicPagingState[it] },
        appendLoading = ((if (state.selectedTabIndex == 0) lessonPagingState else topicPagingState)).loadState.append is LoadState.Loading,
        onItemClicked = {
            if (state.selectedTabIndex == 0)
                lessonPagingState[it]?.let {
                    viewModel.handle(AuthorIntent.AddToQueue(it))
                }
            else
                topicPagingState[it]?.let {
                    env.navigate(Screen.Topic(it))
                }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _AuthorScreen(
    author: Author,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedTabIndex: Int,
    onTabSelected: (index: Int) -> Unit,
    itemCount: Int,
    itemKey: (index: Int) -> Any,
    getLesson: (index: Int) -> Lesson?,
    getTopic: (index: Int) -> Topic?,
    appendLoading: Boolean,
    onItemClicked: (index: Int) -> Unit
) {
    val env = LocalAppEnvironment.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(backgroundBrush()),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { env.popBack() }
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Text(
                        text = author.name,
                        maxLines = 1
                    )
                },
                subtitle = {
                    Text(
                        text = Strings.lesson.quantityString(author.lessonCount),
                        maxLines = 1
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            env.navigate(Screen.Search(author.id, null, selectedTabIndex))
                        }
                    ) {
                        Icon(
                            imageVector = SearchIcon,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .padding(it),
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            SecondaryTabRow(
                modifier = Modifier
                    .padding(end = 12.dp),
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = {
                        Text(
                            text = Strings.lessons.string(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = {
                        Text(
                            text = Strings.topics.string(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .hazeSource(env.hazeState)
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 50.dp
                    ),
                contentPadding = PaddingValues(
                    bottom = BOTTOM_PADDING.dp
                )
            ) {
                items(
                    itemCount,
                    key = itemKey
                ) { index ->
                    if (selectedTabIndex == 0) {
                        val lesson = getLesson(index)
                        if (lesson != null)
                            LessonCell(
                                lesson = lesson,
                                onClick = {
                                    onItemClicked(index)
                                }
                            )
                    } else {
                        val topic = getTopic(index)
                        if (topic != null)
                            TopicCell(
                                topic = topic,
                                onClick = {
                                    onItemClicked(index)
                                }
                            )
                    }
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
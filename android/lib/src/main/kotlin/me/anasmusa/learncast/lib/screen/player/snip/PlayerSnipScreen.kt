package me.anasmusa.learncast.lib.screen.player.snip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.flow.flowOf
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.model.getSampleQueueItem
import me.anasmusa.learncast.data.model.getSampleSnip
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.cell.SnipCell
import me.anasmusa.learncast.lib.core.LocalAppEnvironment
import me.anasmusa.learncast.lib.screen.player.BottomPlayer
import me.anasmusa.learncast.lib.theme.icon.Close
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.ui.player.snip.PlayerSnipIntent
import me.anasmusa.learncast.ui.player.snip.PlayerSnipState
import me.anasmusa.learncast.ui.player.snip.PlayerSnipViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun PlayerSnipScreenPreview() {
    AppTheme {
        _PlayerSnipScreen(
            state =
                PlayerSnipState(
                    currentPlaying = getSampleQueueItem(),
                    snips =
                        flowOf(
                            PagingData.from(
                                MutableList(2) { getSampleSnip(it.toLong()) },
                            ),
                        ),
                ),
            backgroundColors = LocalAppEnvironment.current.backgroundColors,
            onCloseRequested = {},
            sendIntent = {},
        )
    }
}

@Composable
fun PlayerSnipScreen(
    backgroundColors: List<Color>,
    onCloseRequested: () -> Unit,
) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    val viewModel = koinViewModel<PlayerSnipViewModel>(viewModelStoreOwner = owner)
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { owner.viewModelStore.clear() }
    }

    LaunchedEffect(viewModel) {
        state.currentPlaying?.referenceId?.let { lessonId ->
            viewModel.handle(PlayerSnipIntent.Load(lessonId))
        }
    }

    _PlayerSnipScreen(
        state = state,
        backgroundColors = backgroundColors,
        onCloseRequested = onCloseRequested,
        sendIntent = { viewModel.handle(it) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _PlayerSnipScreen(
    state: PlayerSnipState,
    backgroundColors: List<Color>,
    onCloseRequested: () -> Unit,
    sendIntent: (PlayerSnipIntent) -> Unit,
) {
    val gradientStartY = LocalWindowInfo.current.containerSize.height * (-0.5f)
    val gradientEndY = LocalWindowInfo.current.containerSize.height * 0.4f

    var showActionSheet by remember { mutableStateOf<Snip?>(null) }

    val pagingState = state.snips.collectAsLazyPagingItems()
    val currentPlaying = state.currentPlaying

    Scaffold {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            backgroundColors,
                            startY = gradientStartY,
                            endY = gradientEndY,
                        ),
                    ).padding(it),
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                        ).fillMaxWidth(),
            ) {
                FilledIconButton(
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart),
                    onClick = onCloseRequested,
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.2f),
                        ),
                ) {
                    Icon(
                        imageVector = Close,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }

                Text(
                    modifier =
                        Modifier
                            .align(Alignment.Center),
                    text = Strings.SNIPS.string(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            LazyColumn(
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                items(
                    pagingState.itemCount,
                    key = pagingState.itemKey { it.id },
                ) { index ->
                    val snip = pagingState.get(index)
                    if (snip != null) {
                        SnipCell(
                            snip = snip,
                            onClick = {
                                showActionSheet = snip
                            },
                        )
                    }
                }
            }

            if (currentPlaying != null) {
                BottomPlayer(
                    currentPlaying = currentPlaying,
                    currentPositionMs = state.currentPositionMs,
                    playbackState = state.playbackState,
                    backgroundColors = backgroundColors,
                    onClicked = onCloseRequested,
                    togglePlaybackState = {
                        sendIntent(PlayerSnipIntent.TogglePlayback)
                    },
                )
            }
        }

        if (showActionSheet != null) {
            PlayerSnipActionSheet(
                item = showActionSheet!!,
                onDismissRequest = { showActionSheet = null },
                onPlay = {
                    val snip = showActionSheet!!
                    showActionSheet = null
                    sendIntent(PlayerSnipIntent.Play(snip))
                    onCloseRequested()
                },
            )
        }
    }
}

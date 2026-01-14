package me.anasmusa.learncast.lib.screen.player.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.getSampleQueueItem
import me.anasmusa.learncast.lib.AppTheme
import me.anasmusa.learncast.lib.component.cell.QueueItemCell
import me.anasmusa.learncast.lib.component.drag.rememberDragDropState
import me.anasmusa.learncast.lib.core.LocalAppEnvironment
import me.anasmusa.learncast.lib.screen.player.BottomPlayer
import me.anasmusa.learncast.lib.theme.icon.Close
import me.anasmusa.learncast.string
import me.anasmusa.learncast.ui.player.queue.QueueIntent
import me.anasmusa.learncast.ui.player.queue.QueueState
import me.anasmusa.learncast.ui.player.queue.QueueViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun QueueScreenPreview() {
    AppTheme {
        _QueueScreen(
            state =
                QueueState(
                    currentPlaying = getSampleQueueItem(),
                    queuedItems = MutableList(2) { getSampleQueueItem(it.toLong()) },
                ),
            backgroundColors = LocalAppEnvironment.current.backgroundColors,
            onCloseRequested = {},
            sendIntent = {},
        )
    }
}

@Composable
fun QueueScreen(
    backgroundColors: List<Color>,
    onCloseRequested: () -> Unit,
) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    val viewModel = koinViewModel<QueueViewModel>(viewModelStoreOwner = owner)
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { owner.viewModelStore.clear() }
    }

    _QueueScreen(
        state = state,
        backgroundColors = backgroundColors,
        onCloseRequested = onCloseRequested,
        sendIntent = { viewModel.handle(it) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _QueueScreen(
    state: QueueState,
    backgroundColors: List<Color>,
    onCloseRequested: () -> Unit,
    sendIntent: (QueueIntent) -> Unit,
) {
    val gradientStartY = LocalWindowInfo.current.containerSize.height * (-0.5f)
    val gradientEndY = LocalWindowInfo.current.containerSize.height * 0.4f

    val swipeWidth =
        with(LocalDensity.current) {
            96.dp.toPx()
        }
    var swipingId by remember { mutableLongStateOf(-1L) }
    val onSwiping = { id: Long -> swipingId = id }

    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            lazyListState = listState,
            indexOffset = 2,
            onMove = { from, to -> sendIntent(QueueIntent.Move(from, to)) },
        )

    var showActionSheet by remember { mutableStateOf<QueueItem?>(null) }

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
                    text = Strings.QUEUE.string(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            LazyColumn(
                modifier =
                    Modifier
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragDropState.onDrag(offset = offset)
                                },
                                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                                onDragEnd = { dragDropState.onDragInterrupted() },
                                onDragCancel = { dragDropState.onDragInterrupted() },
                            )
                        }.padding(top = 8.dp)
                        .fillMaxWidth()
                        .weight(1f),
                state = listState,
            ) {
                if (currentPlaying != null) {
                    item {
                        SwipeDragBox(
                            id = currentPlaying.id,
                            modifier =
                                Modifier
                                    .padding(
                                        top = 8.dp,
                                        start = 12.dp,
                                        end = 12.dp,
                                    ),
                            swipeWidth = swipeWidth,
                            swipingId = swipingId,
                            onSwiping = onSwiping,
                            onRemove = {
                                swipingId = -1
                                sendIntent(QueueIntent.Remove(currentPlaying.id))
                            },
                            {
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .drawWithContent {
                                                drawRect(backgroundColors.last().copy(alpha = 0.5f))
                                                drawRect(
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    size =
                                                        Size(
                                                            size.width * (state.currentPositionMs.toFloat() / currentPlaying.duration.inWholeMilliseconds),
                                                            size.height,
                                                        ),
                                                )
                                                drawContent()
                                            }.clickable(onClick = {})
                                            .padding(8.dp),
                                ) {
                                    QueueItemCell(
                                        queueItem = currentPlaying,
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = 8.dp,
                                    start = 12.dp,
                                    end = 12.dp,
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Strings.PLAYING_NEXT.string(),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        TextButton(
                            onClick = { sendIntent(QueueIntent.Clear) },
                        ) {
                            Text(
                                text = Strings.CLEAR.string(),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                }

                itemsIndexed(
                    state.queuedItems,
                    key = { _, item -> item.id },
                ) { index, item ->
                    val isDragging = index == dragDropState.draggingItemIndex

                    SwipeDragBox(
                        id = item.id,
                        modifier =
                            Modifier
                                .let {
                                    if (isDragging) {
                                        it
                                            .zIndex(1f)
                                            .graphicsLayer { translationY = dragDropState.draggingItemOffset }
                                    } else if (index == dragDropState.previousIndexOfDraggedItem) {
                                        it
                                            .zIndex(1f)
                                            .graphicsLayer { translationY = dragDropState.previousItemOffset.value }
                                    } else {
                                        it.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                    }
                                }.padding(
                                    top = 8.dp,
                                    start = 12.dp,
                                    end = 12.dp,
                                ).fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = {
                                    showActionSheet = item
                                })
                                .padding(4.dp),
                        swipeWidth = swipeWidth,
                        swipingId = swipingId,
                        onSwiping = onSwiping,
                        onRemove = {
                            sendIntent(QueueIntent.Remove(item.id))
                        },
                    ) {
                        QueueItemCell(
                            queueItem = item,
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
                        sendIntent(QueueIntent.TogglePlayback)
                    },
                )
            }
        }

        if (showActionSheet != null) {
            QueueActionSheet(
                item = showActionSheet!!,
                onDismissRequest = { showActionSheet = null },
                onPlay = {
                    sendIntent(QueueIntent.Play(showActionSheet!!))
                    showActionSheet = null
                },
            )
        }
    }
}

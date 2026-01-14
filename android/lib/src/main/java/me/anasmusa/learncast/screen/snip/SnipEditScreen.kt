package me.anasmusa.learncast.screen.snip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.component.TimeRangeSelector
import me.anasmusa.learncast.component.TimeRangeSelectorState
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.core.formatTime
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.Close
import me.anasmusa.learncast.theme.icon.PlayArrowIcon
import me.anasmusa.learncast.theme.icon.Stop
import me.anasmusa.learncast.ui.snip.SnipEdiIntent
import me.anasmusa.learncast.ui.snip.SnipEditEvent
import me.anasmusa.learncast.ui.snip.SnipEditState
import me.anasmusa.learncast.ui.snip.SnipEditViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun SnipEditScreenPreview() {
    AppTheme {
        _SnipEditScreen(
            clientSnipId = "",
            queueItemId = 0L,
            state =
                SnipEditState(
                    STATE_LOADING,
                    false,
                ),
            rangeSelectorState =
                TimeRangeSelectorState(
                    0,
                    11,
                    60 * 60,
                ),
            note = "adgdsgerfg",
            colors = LocalAppEnvironment.current.backgroundColors,
            onDismissRequest = {},
            onNoteChanged = {},
            sendIntent = {},
        )
    }
}

@Composable
fun SnipEditScreen(
    clientSnipId: String,
    queueItemId: Long,
    startAt: Long,
    endAt: Long,
    duration: Long,
    audioPath: String,
    note: String?,
    colors: List<Color>,
    onDismissRequest: (saved: Boolean) -> Unit,
) {
    val owner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }
    val viewModel = koinViewModel<SnipEditViewModel>(viewModelStoreOwner = owner)

    val state by viewModel.state.collectAsState()
    var noteState by rememberSaveable { mutableStateOf(note ?: "") }

    DisposableEffect(viewModel) {
        viewModel.handle(
            SnipEdiIntent.Init(
                clientSnipId = clientSnipId,
                audioPath = audioPath,
                startPosition = startAt,
            ),
        )
        onDispose { owner.viewModelStore.clear() }
    }

    LaunchedEffect(viewModel) {
        viewModel.subscribe {
            when (it) {
                SnipEditEvent.Finish ->
                    onDismissRequest(true)
                is SnipEditEvent.OnSnipLoaded -> {
                    noteState = it.note ?: ""
                }
                is SnipEditEvent.ShowError -> {}
            }
        }
    }

    val rangeSelectorState =
        rememberSaveable(
            saver =
                Saver(save = { "${it.start},${it.end},${it.total}" }, restore = {
                    val s = it.split(',')
                    TimeRangeSelectorState(s[0].toInt(), s[1].toInt(), s[2].toInt())
                }),
        ) {
            TimeRangeSelectorState(
                (startAt / 1000).toInt(),
                (endAt / 1000).toInt(),
                (duration / 1000).toInt(),
            )
        }

    LaunchedEffect(Unit) {
        launch {
            snapshotFlow { rangeSelectorState.start }
                .drop(1)
                .debounce(1000)
                .collect {
                    viewModel.handle(
                        SnipEdiIntent.Start(
                            rangeSelectorState.start,
                            rangeSelectorState.start + 5,
                        ),
                    )
                }
        }
        launch {
            snapshotFlow { rangeSelectorState.end }
                .drop(1)
                .debounce(1000)
                .collect {
                    viewModel.handle(
                        SnipEdiIntent.Start(
                            rangeSelectorState.end - 5,
                            rangeSelectorState.end,
                        ),
                    )
                }
        }
    }

    _SnipEditScreen(
        clientSnipId = clientSnipId,
        queueItemId = queueItemId,
        state = state,
        rangeSelectorState = rangeSelectorState,
        note = noteState,
        colors = colors,
        onDismissRequest = onDismissRequest,
        onNoteChanged = {
            noteState = it
        },
        sendIntent = {
            viewModel.handle(it)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _SnipEditScreen(
    clientSnipId: String,
    queueItemId: Long,
    state: SnipEditState,
    rangeSelectorState: TimeRangeSelectorState,
    note: String,
    colors: List<Color>,
    onDismissRequest: (saved: Boolean) -> Unit,
    onNoteChanged: (value: String) -> Unit,
    sendIntent: (SnipEdiIntent) -> Unit,
) {
    val duration by remember {
        derivedStateOf {
            rangeSelectorState.end - rangeSelectorState.start
        }
    }

    val gradientStartY = LocalWindowInfo.current.containerSize.height * (-0.4f)
    val gradientEndY = LocalWindowInfo.current.containerSize.height * 0.2f

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { onDismissRequest(false) },
        dragHandle = null,
        sheetGesturesEnabled = false,
        containerColor = Color.Transparent,
    ) {
        Column(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors,
                                startY = gradientStartY,
                                endY = gradientEndY,
                            ),
                    ).padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
            ) {
                Text(
                    modifier =
                        Modifier
                            .align(Alignment.Center),
                    text = formatTime(duration),
                )

                FilledIconButton(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd),
                    onClick = { onDismissRequest(false) },
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                        ),
                ) {
                    Icon(
                        imageVector = Close,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            TimeRangeSelector(
                modifier = Modifier.padding(top = 16.dp),
                state = rangeSelectorState,
                color = Color.White.copy(alpha = 0.3f),
                currentPosition =
                    if (state.playbackState == STATE_PLAYING) {
                        (state.currentPositionMs / 1000).toInt()
                    } else {
                        -1
                    },
            )

            TextField(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                value = note,
                onValueChange = {
                    if (it.length <= 128) {
                        onNoteChanged(it)
                    }
                },
                label = {
                    Text(
                        text = Strings.WRITE_NOTE.string(),
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        unfocusedContainerColor = lerp(colors.first(), colors.last(), 0.8f),
                        focusedContainerColor = lerp(colors.first(), colors.last(), 0.8f),
                    ),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (state.playbackState == STATE_LOADING) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .padding(start = 20.dp),
                    )
                } else {
                    TextButton(
                        border = BorderStroke(1.dp, LocalContentColor.current),
                        onClick = {
                            sendIntent(
                                if (state.playbackState == STATE_PLAYING) {
                                    SnipEdiIntent.Stop
                                } else {
                                    SnipEdiIntent.Start(
                                        rangeSelectorState.start,
                                        rangeSelectorState.end,
                                    )
                                },
                            )
                        },
                    ) {
                        Icon(
                            modifier =
                                Modifier
                                    .padding(end = 8.dp),
                            imageVector =
                                if (state.playbackState == STATE_PLAYING) {
                                    Stop
                                } else {
                                    PlayArrowIcon
                                },
                            contentDescription = null,
                        )
                        Text(
                            text =
                                if (state.playbackState == STATE_PLAYING) {
                                    Strings.STOP.string()
                                } else {
                                    Strings.PLAY.string()
                                },
                        )
                    }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .padding(end = 20.dp),
                    )
                } else {
                    Button(
                        modifier = Modifier,
                        onClick = {
                            sendIntent(
                                SnipEdiIntent.Save(
                                    clientSnipId,
                                    queueItemId,
                                    rangeSelectorState.start,
                                    rangeSelectorState.end,
                                    note,
                                ),
                            )
                        },
                    ) {
                        Text(
                            text = Strings.SAVE.string(),
                        )
                    }
                }
            }
        }
    }
}

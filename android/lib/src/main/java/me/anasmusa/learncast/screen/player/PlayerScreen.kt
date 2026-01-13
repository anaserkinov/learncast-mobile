package me.anasmusa.learncast.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.component.Loader
import me.anasmusa.learncast.component.QueueButton
import me.anasmusa.learncast.core.LocalAppEnvironment
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.formatTime
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.model.getSampleQueueItem
import me.anasmusa.learncast.screen.player.queue.QueueScreen
import me.anasmusa.learncast.screen.player.snip.PlayerSnipScreen
import me.anasmusa.learncast.screen.snip.SnipEditScreen
import me.anasmusa.learncast.string
import me.anasmusa.learncast.theme.icon.ArrowDown
import me.anasmusa.learncast.theme.icon.CutIcon
import me.anasmusa.learncast.theme.icon.Forward30
import me.anasmusa.learncast.theme.icon.MoreVert
import me.anasmusa.learncast.theme.icon.Pause
import me.anasmusa.learncast.theme.icon.PlayArrowIcon
import me.anasmusa.learncast.theme.icon.Replay10
import me.anasmusa.learncast.ui.player.PlayerEvent
import me.anasmusa.learncast.ui.player.PlayerIntent
import me.anasmusa.learncast.ui.player.PlayerState
import me.anasmusa.learncast.ui.player.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Preview
@Composable
private fun PlayerScreenPreview() {
    val density = LocalDensity.current
    val collapsedPx = with(density) { (80 + 64).dp.toPx() }
    val expandedPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val windowBottomInset = NavigationBarDefaults.windowInsets.getBottom(density)
    AppTheme {
        _PlayerScreen(
            modifier = Modifier,
            state = PlayerState(
                currentPlaying = getSampleQueueItem(),
                snipCount = 100
            ),
            draggableState = AnchoredDraggableState(
                initialValue = "expanded",
                anchors = DraggableAnchors {
                    "expanded" at 0f
                    "collapsed" at (expandedPx - collapsedPx - windowBottomInset)
                }
            ),
            sendIntent = {}
        )
    }
}

@Preview
@Composable
private fun CollapsedPlayerPreview() {
    AppTheme {
        CollapsedPlayer(
            offset = 1f,
            scope = rememberCoroutineScope(),
            draggableState = AnchoredDraggableState(initialValue = "expanded"),
            color = lerp(
                LocalAppEnvironment.current.playerBackgroundColors.first(),
                LocalAppEnvironment.current.playerBackgroundColors.last(),
                0.7f
            ),
            currentPlaying = getSampleQueueItem(),
            playbackState = STATE_LOADING,
            currentPositionMs = 35.toDuration(DurationUnit.MINUTES).inWholeMilliseconds,
            queuedCount = 24,
            changePlayPause = {},
            openQueueScreen = {}
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CollapsedPlayer(
    offset: Float,
    scope: CoroutineScope,
    draggableState: AnchoredDraggableState<String>,
    color: Color,
    currentPlaying: QueueItem,
    playbackState: Int,
    currentPositionMs: Long,
    queuedCount: Int,
    changePlayPause: () -> Unit,
    openQueueScreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .alpha(1 - (1 - offset) / 0.2f)
            .clickable(onClick = {
                scope.launch {
                    draggableState.animateTo("expanded")
                }
            })
            .background(color, RoundedCornerShape(6.dp))
    ) {
        LinearProgressIndicator(
            progress = { currentPositionMs.toFloat() / currentPlaying.duration.inWholeMilliseconds },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                .size(55.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6)),
                model = if (currentPlaying.coverImagePath != null)
                    currentPlaying.coverImagePath!!.normalizeUrl()
                else
                    appConfig.mainLogo,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(Color.Black.copy(0.3f), BlendMode.SrcAtop),
                contentDescription = null
            )

            if (playbackState == STATE_LOADING)
                CircularWavyProgressIndicator(
                    modifier = Modifier
                        .size(28.dp)
                )
            else
                IconButton(
                    modifier = Modifier
                        .fillMaxSize(),
                    onClick = changePlayPause,
                    shape = RectangleShape
                ) {
                    Icon(
                        modifier = Modifier
                            .size(28.dp),
                        imageVector = if (playbackState == STATE_PAUSED) {
                            PlayArrowIcon
                        } else
                            Pause,
                        contentDescription = null
                    )
                }
        }
        Column(
            modifier = Modifier
                .padding(start = 80.dp, end = 60.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentPlaying.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modifier = Modifier
                    .alpha(0.7f),
                text = currentPlaying.subTitle,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        QueueButton(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(56.dp)
                .align(Alignment.CenterEnd),
            count = queuedCount,
            onClick = openQueueScreen
        )
    }
}


private fun Int.darken(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)

    // increase saturation
    hsv[1] = min(1f, hsv[1] + amount)

    // optional: tiny brightness reduction (NOT black)
    hsv[2] = max(0f, hsv[2] * (1f - amount * 0.6f))

    return Color(android.graphics.Color.HSVToColor(hsv))
}


private fun Int.lighten(amount: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this, hsv)

    hsv[2] = min(1f, hsv[2] + hsv[2] * amount)

    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun PlayerScreen(
    modifier: Modifier,
    draggableState: AnchoredDraggableState<String>
) {
    val viewModel = koinViewModel<PlayerViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel, draggableState) {
        viewModel.subscribe {
            when (it) {
                PlayerEvent.ShowPlayer -> {
                    draggableState.animateTo("expanded")
                }
            }
        }
    }

    _PlayerScreen(
        modifier = modifier,
        state = state,
        draggableState = draggableState,
        sendIntent = {
            viewModel.handle(it)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _PlayerScreen(
    modifier: Modifier,
    state: PlayerState,
    draggableState: AnchoredDraggableState<String>,
    sendIntent: (intent: PlayerIntent) -> Unit
) {
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val windowInfo = LocalWindowInfo.current

    val totalHeight = windowInfo.containerSize.height
    val offset = draggableState.offset / draggableState.anchors.maxPosition()

    val statusBarOffset: Dp
    val bottomNavigationBarOffset: Dp
    with(LocalDensity.current) {
        statusBarOffset = WindowInsets.statusBars.getTop(this).toDp()
        bottomNavigationBarOffset = WindowInsets.navigationBars.getBottom(this).toDp()
    }

    var animationScreen by remember { mutableStateOf(false) }
    var showScreen by remember(draggableState) { mutableIntStateOf(0) }

    var showSnipEditScreen by rememberSaveable { mutableStateOf(false) }
    var showActionSheet by remember { mutableStateOf(false) }


    var colors by remember { mutableStateOf(env.playerBackgroundColors) }
    var collapsedColor by remember {
        mutableStateOf(
            lerp(
                env.playerBackgroundColors.first(),
                env.playerBackgroundColors.last(),
                0.7f
            )
        )
    }
    val gradientStartY = windowInfo.containerSize.height * (-0.5f)
    val gradientEndY = windowInfo.containerSize.height * 0.4f

    LaunchedEffect(state, draggableState) {
        snapshotFlow { state }
            .combine(snapshotFlow { draggableState.settledValue }) { state, settledValue ->
                if (state.currentPlaying == null && settledValue != "collapsed") {
                    animationScreen = false
                    showScreen = 0
                    draggableState.snapTo("collapsed")
                }
                if (state.snipCount == -1 && state.currentPlaying?.referenceType == ReferenceType.LESSON && settledValue == "expanded")
                    sendIntent(PlayerIntent.LoadSnipCount)
            }.collect()
    }

    BackHandler(enabled = showScreen != 0 || draggableState.currentValue == "expanded") {
        when {
            showScreen != 0 -> {
                showScreen = 0
            }

            draggableState.currentValue == "expanded" -> {
                scope.launch {
                    draggableState.animateTo("collapsed")
                }
            }
        }
    }

    val currentPlaying = state.currentPlaying

    if (currentPlaying != null) {
        LaunchedEffect(currentPlaying.coverImagePath) {
            if (currentPlaying.coverImagePath != null) {
                val loader = ImageLoader(context)

                val req = ImageRequest.Builder(context)
                    .data(currentPlaying.coverImagePath!!.normalizeUrl())
                    .size(112)
                    .allowHardware(false)
                    .build()

                loader.execute(req)
                    .image?.toBitmap()?.let {
                        val palette = Palette.from(it).generate()
                        yield()
                        val vibrant = palette.getVibrantColor(
                            palette.getDarkVibrantColor(
                                palette.getDominantColor(0)
                            )
                        )
                        colors = if (vibrant != 0)
                            listOf(
                                vibrant.lighten(0.3f),
                                vibrant.darken(0.8f)
                            )
                        else
                            env.playerBackgroundColors
                        collapsedColor = lerp(
                            colors.first(),
                            colors.last(),
                            0.7f
                        )
                    }
            } else {
                colors = env.playerBackgroundColors
                collapsedColor = lerp(
                    colors.first(),
                    colors.last(),
                    0.7f
                )
            }
        }

        Box(
            modifier = modifier
                .padding(
                    start = (4 * offset).dp,
                    end = (4 * offset).dp
                )
                .fillMaxWidth()
                .offset { IntOffset(0, draggableState.offset.roundToInt()) }
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    enabled = showScreen == 0
                )
                .height(
                    (totalHeight - (totalHeight - 64) * offset).dp
                )
                .background(
                    Brush.verticalGradient(colors, startY = gradientStartY, endY = gradientEndY),
                    shape = RoundedCornerShape((6 * offset).dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {

            if (offset >= 0.8f)
                CollapsedPlayer(
                    offset = offset,
                    scope = scope,
                    draggableState = draggableState,
                    color = collapsedColor,
                    currentPositionMs = state.currentPositionMs,
                    currentPlaying = currentPlaying,
                    playbackState = state.playbackState,
                    queuedCount = state.queuedCount,
                    changePlayPause = {
                        sendIntent(PlayerIntent.TogglePlaybackState)
                    },
                    openQueueScreen = {
                        scope.launch {
                            animationScreen = false
                            showScreen = 1
                            draggableState.animateTo("expanded")
                        }
                    }
                )

            Column(
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .padding(
                        start = 12.dp,
                        top = statusBarOffset,
                        end = 12.dp,
                        bottom = bottomNavigationBarOffset
                    )
                    .alpha(1 - offset / 0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledIconButton(
                        modifier = Modifier,
                        onClick = {
                            scope.launch {
                                draggableState.animateTo("collapsed")
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            imageVector = ArrowDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            showActionSheet = true
                        }
                    ) {
                        Icon(
                            imageVector = MoreVert,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }


                AsyncImage(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    model = if (currentPlaying.coverImagePath != null)
                        currentPlaying.coverImagePath!!.normalizeUrl()
                    else
                        appConfig.mainLogo,
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawFadedEdge(leftEdge = true)
                            drawFadedEdge(leftEdge = false)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .basicMarquee(spacing = MarqueeSpacing(36.dp)),
                        text = currentPlaying.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        modifier = Modifier
                            .alpha(0.7f)
                            .basicMarquee(spacing = MarqueeSpacing(36.dp)),
                        text = currentPlaying.subTitle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                var sliderPositionMs by remember { mutableLongStateOf(0L) }
                var isUserDragging by remember { mutableStateOf(false) }
                LaunchedEffect(state.currentPositionMs) {
                    if (!isUserDragging) {
                        sliderPositionMs = state.currentPositionMs
                    }
                }
                val sliderColors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTickColor = Color.White.copy(alpha = 0.2f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                )
                Slider(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    value = (sliderPositionMs / 1000).toFloat(),
                    onValueChange = {
                        isUserDragging = true
                        sliderPositionMs = (it * 1000).toLong()
                    },
                    onValueChangeFinished = {
                        isUserDragging = false
                        sendIntent(PlayerIntent.SeekTo(sliderPositionMs))
                    },
                    valueRange = 0f..currentPlaying.duration.inWholeSeconds.toFloat(),
                    colors = sliderColors,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = remember { MutableInteractionSource() },
                            colors = sliderColors,
                            thumbSize = DpSize(4.dp, 24.dp)
                        )
                    },
                    track = {
                        SliderDefaults.Track(
                            colors = sliderColors,
                            sliderState = it,
                            drawTick = { offset, color ->
                                drawStopIndicator(
                                    offset = offset,
                                    color = color,
                                    size = 2.dp
                                )
                            }
                        )
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-8).dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(sliderPositionMs),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatTime(currentPlaying.duration.inWholeMilliseconds),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Spacer(
                        modifier = Modifier
                            .size(56.dp)
                    )

                    IconButton(
                        modifier = Modifier
                            .size(56.dp),
                        onClick = { sendIntent(PlayerIntent.Seek(false)) }
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(36.dp),
                            imageVector = Replay10,
                            contentDescription = null
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.playbackState == STATE_LOADING)
                            CircularWavyProgressIndicator()
                        else
                            Button(
                                modifier = Modifier
                                    .size(56.dp),
                                onClick = {
                                    sendIntent(PlayerIntent.TogglePlaybackState)
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(36.dp),
                                    imageVector = if (state.playbackState == STATE_PAUSED)
                                        PlayArrowIcon
                                    else
                                        Pause,
                                    contentDescription = null
                                )
                            }
                    }
                    IconButton(
                        modifier = Modifier
                            .size(56.dp),
                        onClick = { sendIntent(PlayerIntent.Seek(true)) }
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(36.dp),
                            imageVector = Forward30,
                            contentDescription = null
                        )
                    }

                    QueueButton(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(56.dp),
                        count = state.queuedCount,
                        onClick = {
                            animationScreen = true
                            showScreen = 1
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .align(Alignment.Center),
                        onClick = {
                            sendIntent(PlayerIntent.Pause)
                            showSnipEditScreen = true
                        },
                        contentPadding = ButtonDefaults.contentPaddingFor(64.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = CutIcon,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 8.dp),
                            text = if (currentPlaying.referenceUuid.isNotEmpty())
                                me.anasmusa.learncast.Strings.update_snip.string()
                            else me.anasmusa.learncast.Strings.create_snip.string(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (currentPlaying.referenceType == ReferenceType.LESSON)
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(48.dp)
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    onClick = {
                                        animationScreen = true
                                        showScreen = 2
                                    }
                                )
                                .background(
                                    Color.White.copy(0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.snipCount == -1)
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.padding(8.dp),
                                    stroke = Stroke(
                                        width = with(LocalDensity.current) { 3.dp.toPx() },
                                        cap = StrokeCap.Round,
                                    )
                                )
                            else
                                Text(
                                    text = if (state.snipCount <= 99)
                                        state.snipCount.toString()
                                    else
                                        "99+",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 20.sp
                                )
                        }
                }
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxSize(),
                visible = showScreen != 0,
                enter = if (animationScreen) slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it }
                ) else EnterTransition.None,
                exit = if (animationScreen) slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { it }
                ) else ExitTransition.None
            ) {
                if (showScreen == 1)
                    QueueScreen(
                        backgroundColors = colors,
                        onCloseRequested = {
                            showScreen = 0
                        }
                    )
                else
                    PlayerSnipScreen(
                        backgroundColors = colors,
                        onCloseRequested = {
                            showScreen = 0
                        }
                    )
            }
        }

        if (showSnipEditScreen)
            SnipEditScreen(
                clientSnipId = currentPlaying.referenceUuid,
                queueItemId = currentPlaying.id,
                startAt = (currentPlaying.startMs ?: 0) + state.currentPositionMs,
                endAt = if (currentPlaying.endMs != null)
                    currentPlaying.endMs!!
                else
                    min(
                        state.currentPositionMs + 5.toDuration(DurationUnit.MINUTES).inWholeMilliseconds,
                        currentPlaying.audioDuration.inWholeMilliseconds
                    ),
                duration = currentPlaying.audioDuration.inWholeMilliseconds,
                audioPath = currentPlaying.audioPath,
                note = null,
                colors = colors,
                onDismissRequest = {
                    showSnipEditScreen = false
                    if (it) sendIntent(PlayerIntent.Refresh)
                }
            )

        if (showActionSheet)
            PlayerActionSheet(
                isSnip = currentPlaying.referenceType == ReferenceType.SNIP,
                downloadState = currentPlaying.downloadState,
                percentDownloaded = currentPlaying.percentDownloaded,
                isCompleted = currentPlaying.status == UserProgressStatus.COMPLETED,
                isFavourite = currentPlaying.isFavourite,
                onDismissRequest = { showActionSheet = false },
                onDownloadClicked = {
                    sendIntent(PlayerIntent.Download)
                },
                onCompletedClicked = {
                    sendIntent(PlayerIntent.ToggleCompletedState)
                },
                onFavouriteClicked = {
                    sendIntent(PlayerIntent.ToggleFavourite)
                },
                onDeleteClicked = {
                    sendIntent(PlayerIntent.DeleteSnip)
                }
            )

        if (state.isLoading)
            Loader()
    }
}

private fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
    val edgeWidthPx = 32.dp.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush =
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startX = if (leftEdge) 0f else size.width,
                endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx,
            ),
        blendMode = BlendMode.DstIn
    )
}
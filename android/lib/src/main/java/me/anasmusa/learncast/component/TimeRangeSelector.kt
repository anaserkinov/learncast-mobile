package me.anasmusa.learncast.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderDefaults.drawStopIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.anasmusa.learncast.AppTheme
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.formatTime
import me.anasmusa.learncast.string
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

@Stable
class TimeRangeSelectorState(
    initialStart: Int,
    initialEnd: Int,
    val total: Int
) {
    var start by mutableIntStateOf(initialStart)
    var end by mutableIntStateOf(initialEnd)
}


@Preview
@Composable
fun TimeRangeSelectorPreview() {
    val state = remember { TimeRangeSelectorState(0, 10, 2000) }
    AppTheme {
        TimeRangeSelector(
            state = state,
            color = MaterialTheme.colorScheme.secondary,
            currentPosition = 100
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    modifier: Modifier = Modifier,
    state: TimeRangeSelectorState,
    color: Color,
    currentPosition: Int
) {

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemWidthPx = with(density) { 10.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()

    var spinnerWidth by remember { mutableFloatStateOf(20 * itemWidthPx) }

    val horizontalOffset = spinnerWidth / 2 - itemWidthPx / 2

    var startSecondsState by remember { mutableIntStateOf(state.start) }
    var endSecondsState by remember { mutableIntStateOf(state.end) }

    val startSpinnerListState = remember(spinnerWidth) {
        LazyListState(state.start)
    }
    val endSpinnerListState = remember(spinnerWidth) {
        LazyListState(state.end)
    }

    var sliderState by remember {
        mutableStateOf(
            (state.start / 60).toFloat().rangeTo(
                ceil(state.end / 60f).toInt().toFloat()
            )
        )
    }

    LaunchedEffect(startSpinnerListState) {
        snapshotFlow { startSpinnerListState.layoutInfo.visibleItemsInfo }
            .collect { itemInfos ->
                val selectedSecond = itemInfos.binarySearch { it.offset.compareTo(0) }.let {
                    if (it < 0) abs(it + 1)
                    else it
                }.let { itemInfos[it].index }
                if (selectedSecond + 10 > endSecondsState)
                    startSpinnerListState.requestScrollToItem(endSecondsState - 10)
                else {
                    startSecondsState = selectedSecond
                    state.start = startSecondsState
                    sliderState =
                        (startSecondsState / 60).toFloat().rangeTo(sliderState.endInclusive)
                }
            }
    }
    LaunchedEffect(endSpinnerListState) {
        snapshotFlow { endSpinnerListState.layoutInfo.visibleItemsInfo }
            .collect { itemInfos ->
                val selectedSecond = itemInfos.binarySearch { it.offset.compareTo(0) }.let {
                    if (it < 0) abs(it + 1)
                    else it
                }.let { itemInfos[it].index }
                if (selectedSecond - 10 < startSecondsState)
                    endSpinnerListState.requestScrollToItem(startSecondsState + 10)
                else {
                    endSecondsState = selectedSecond
                    state.end = endSecondsState
                    sliderState =
                        sliderState.start.rangeTo(ceil(endSecondsState / 60f).toInt().toFloat())
                }
            }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTickColor = Color.White.copy(alpha = 0.2f),
        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
    )

    fun thumb() = @Composable { _: RangeSliderState ->
        SliderDefaults.Thumb(
            interactionSource = remember { MutableInteractionSource() },
            colors = sliderColors,
            thumbSize = DpSize(4.dp, 24.dp)
        )
    }
    Column(
        modifier = modifier
    ) {
        RangeSlider(
            modifier = Modifier.drawWithContent {
                drawContent()
                if (currentPosition != -1)
                    drawCircle(
                        color = Color.Red,
                        radius = 2.dp.toPx(),
                        center = Offset(
                            x = currentPosition * size.width / state.total,
                            y = size.height - 6.dp.toPx()
                        )
                    )
            },
            value = sliderState,
            onValueChange = {
                var start = it.start.toInt()
                var end = it.endInclusive.toInt()
                val startChanged = sliderState.start.toInt() != start
                if (start == end) {
                    if (startChanged)
                        start = end - 1
                    else
                        end = start + 1
                }
                sliderState = start.toFloat()..end.toFloat()
                if (startChanged) {
                    startSecondsState = min(
                        start * 60,
                        sliderState.start.toInt() * 60
                    )
                    state.start = endSecondsState
                    coroutineScope.launch {
                        startSpinnerListState.scrollToItem(startSecondsState)
                    }
                } else {
                    endSecondsState = min(
                        end * 60,
                        state.total
                    )
                    state.end = endSecondsState
                    coroutineScope.launch {
                        endSpinnerListState.scrollToItem(endSecondsState)
                    }
                }
            },
            valueRange = 0f..ceil(state.total / 60f).toInt().toFloat(),
            steps = ceil(state.total / 60f).toInt() - 1,
            colors = sliderColors,
            startThumb = thumb(),
            endThumb = thumb(),
            track = {
                SliderDefaults.Track(
                    colors = sliderColors,
                    rangeSliderState = it,
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
        ) {
            TimeSpinner(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned {
                        spinnerWidth = it.size.width.toFloat()
                    },
                startSecondsState,
                end = endSecondsState,
                total = state.total,
                currentPosition = currentPosition,
                title = Strings.start,
                color = color,
                isStart = true,
                horizontalOffset = with(density) { horizontalOffset.toDp() },
                listState = startSpinnerListState,
                textMeasurer = textMeasurer
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            TimeSpinner(
                modifier = Modifier
                    .weight(1f),
                endSecondsState,
                end = startSecondsState,
                total = state.total,
                currentPosition = currentPosition,
                title = Strings.end,
                color = color,
                isStart = false,
                horizontalOffset = with(density) { horizontalOffset.toDp() },
                listState = endSpinnerListState,
                textMeasurer = textMeasurer
            )
        }
    }
}

@Composable
private fun TimeSpinner(
    modifier: Modifier,
    value: Int,
    end: Int,
    total: Int,
    currentPosition: Int,
    title: Int,
    color: Color,
    isStart: Boolean,
    horizontalOffset: Dp,
    listState: LazyListState,
    textMeasurer: TextMeasurer
) {
    val labelSmall = MaterialTheme.typography.labelSmall
        .copy(color = Color.White.copy(0.6f), fontSize = 12.sp)

    val density = LocalDensity.current

    val itemWidthPx = with(density) { 10.dp.toPx() }
    val itemWidthDp = 10.dp

    val smallBarWidth = with(density) { 1.dp.toPx() }
    val smallBarHeight = with(density) { 10.dp.toPx() }
    val bigBarWidth = with(density) { 2.dp.toPx() }
    val bigBarHeight = with(density) { 15.dp.toPx() }

    val fadedEdgeWidth = with(density) { 32.dp.toPx() }

    val flingBehavior = rememberSnapFlingBehavior(listState, snapPosition = SnapPosition.Start)

    val endOffset by remember(end, listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.find {
                it.index == end
            }?.offset?.toFloat()
        }
    }
    val currentOffset by remember(currentPosition, listState) {
        derivedStateOf {
            if (currentPosition == -1) null
            else
                listState.layoutInfo.visibleItemsInfo.find {
                    it.index == currentPosition
                }?.offset?.toFloat()
        }
    }


    Column(
        modifier = modifier
    ) {
        Text(
            text = title.string(),
            style = MaterialTheme.typography.titleMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = color,
                        start = Offset(size.width / 2, size.height - bigBarHeight),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = bigBarWidth
                    )
                    if (endOffset != null) {
                        drawLine(
                            color = color,
                            start = Offset(
                                size.width / 2 + endOffset!!,
                                size.height - bigBarHeight
                            ),
                            end = Offset(size.width / 2 + endOffset!!, size.height),
                            strokeWidth = bigBarWidth
                        )
                    }
                    if (currentOffset != null) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(
                                size.width / 2 + currentOffset!!,
                                size.height - bigBarHeight
                            ),
                            end = Offset(size.width / 2 + currentOffset!!, size.height),
                            strokeWidth = bigBarWidth
                        )
                    }
                    if (isStart)
                        drawRect(
                            color = color.copy(alpha = 0.4f),
                            topLeft = Offset(
                                size.width / 2,
                                size.height - bigBarHeight
                            ),
                            size = Size(
                                if (endOffset != null)
                                    endOffset!!
                                else
                                    size.width / 2,
                                bigBarHeight
                            )
                        )
                    else
                        drawRect(
                            color = color.copy(alpha = 0.4f),
                            topLeft = Offset(
                                if (endOffset == null) 0f else size.width / 2 + endOffset!!,
                                size.height - bigBarHeight
                            ),
                            size = Size(
                                if (endOffset != null)
                                    abs(endOffset!!)
                                else
                                    size.width / 2,
                                bigBarHeight
                            )
                        )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.Black),
                            startX = if (isStart) size.width else 0f,
                            endX = if (isStart) size.width - fadedEdgeWidth else fadedEdgeWidth
                        ),
                        topLeft = Offset(
                            if (isStart) size.width - fadedEdgeWidth else 0f,
                            size.height - bigBarHeight
                        ),
                        size = Size(
                            fadedEdgeWidth,
                            bigBarHeight
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {

            Text(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                text = formatTime(value),
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(
                    horizontal = horizontalOffset
                )
            ) {
                items(total + 1) { second ->
                    Canvas(
                        modifier = Modifier
                            .width(itemWidthDp)
                            .height(64.dp)
                    ) {
                        val isTen = second % 10 == 0
                        val barHeight = (if (isTen) bigBarHeight else smallBarHeight)
                        if (isTen) {
                            val layoutResult = textMeasurer.measure(
                                formatTime(second),
                                style = labelSmall
                            )
                            drawText(
                                layoutResult,
                                topLeft = Offset(
                                    (itemWidthPx - layoutResult.size.width) / 2,
                                    size.height - barHeight - layoutResult.size.height
                                )
                            )
                        }
                        drawLine(
                            color = Color.White,
                            start = Offset(itemWidthPx / 2, size.height - barHeight),
                            end = Offset(itemWidthPx / 2, size.height),
                            strokeWidth = if (isTen) bigBarWidth else smallBarWidth
                        )
                    }
                }
            }
        }
    }
}
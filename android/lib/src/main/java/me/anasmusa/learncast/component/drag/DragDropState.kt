package me.anasmusa.learncast.component.drag

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Composable
fun rememberDragDropState(lazyListState: LazyListState, indexOffset: Int = 0, onMove: (Int, Int) -> Unit): DragDropState {
    val scope = rememberCoroutineScope()
    val state =
        remember(lazyListState) {
            DragDropState(state = lazyListState, indexOffset = indexOffset, onMove = onMove, scope = scope)
        }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}

class DragDropState
internal constructor(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val indexOffset: Int = 0,
    private val onMove: (Int, Int) -> Unit,
) {
    var draggingItemIndex by mutableIntStateOf(-1)
        private set

    internal val scrollChannel = Channel<Float>()

    private var dOffset = 0
    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex + indexOffset }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    internal var previousItemOffset = Animatable(0f)
        private set

    internal fun onDragStart(offset: Offset) {
        dOffset = state.layoutInfo.visibleItemsInfo[indexOffset].offset
        for (i in indexOffset until state.layoutInfo.visibleItemsInfo.size){
            val item = state.layoutInfo.visibleItemsInfo[i]
            if (offset.y.toInt() in item.offset..(item.offset + item.size) ){
                draggingItemIndex = item.index - indexOffset
                draggingItemInitialOffset = item.offset
                break
            }
        }
    }

    internal fun onDragInterrupted() {
        if (draggingItemIndex != -1) {
            previousIndexOfDraggedItem = draggingItemIndex
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(
                    0f,
                    spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 1f),
                )
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemDraggedDelta = 0f
        draggingItemIndex = -1
        draggingItemInitialOffset = 0
    }

    internal fun onDrag(offset: Offset) {
        draggingItemDraggedDelta += offset.y

        val draggingItem = draggingItemLayoutInfo ?: return
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + (endOffset - startOffset) / 2f

        var targetItem: LazyListItemInfo? = null
        for (i in 0 until state.layoutInfo.visibleItemsInfo.size){
            val item = state.layoutInfo.visibleItemsInfo[i]
            if (item.index != draggingItem.index &&
                item.index >= indexOffset &&
                middleOffset.toInt() in item.offset..(item.offset + item.size) ){
                targetItem = item
                break
            }
        }
        if (targetItem != null) {
            if (
                draggingItem.index == state.firstVisibleItemIndex ||
                targetItem.index == state.firstVisibleItemIndex
            ) {
                state.requestScrollToItem(
                    state.firstVisibleItemIndex,
                    state.firstVisibleItemScrollOffset,
                )
            }
            onMove.invoke(draggingItem.index - indexOffset, targetItem.index - indexOffset)
            draggingItemIndex = targetItem.index - indexOffset
        } else {
            val overscroll =
                when {
                    draggingItemDraggedDelta > 0 ->
                        (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                    draggingItemDraggedDelta < 0 ->
                        (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                    else -> 0f
                }
            if (overscroll != 0f) {
                scrollChannel.trySend(overscroll)
            }
        }
    }

    private val LazyListItemInfo.offsetEnd: Int
        get() = this.offset + this.size
}
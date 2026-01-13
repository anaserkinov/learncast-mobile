package me.anasmusa.learncast.screen.player.queue

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.anasmusa.learncast.theme.icon.PlaylistRemove
import kotlin.math.roundToInt

@Composable
fun SwipeDragBox(
    id: Long,
    modifier: Modifier = Modifier,
    swipeWidth: Float,
    swipingId: Long,
    onSwiping: (Long) -> Unit,
    onRemove: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = 0,
            anchors = DraggableAnchors {
                0 at 0f
                1 at swipeWidth
            }
        )
    }

    val isOpen by remember {
        derivedStateOf { draggableState.progress(0, 1) > 0.1f }
    }
    LaunchedEffect(isOpen) {
        if (isOpen) onSwiping(id)
    }
    LaunchedEffect(swipingId) {
        if (swipingId != id && draggableState.currentValue == 1)
            draggableState.animateTo(0)
    }


    Box(
        modifier = modifier
    ) {
        if (isOpen)
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier
                        .size(56.dp),
                    onClick = onRemove
                ) {
                    Icon(
                        imageVector = PlaylistRemove,
                        contentDescription = null,
                        tint = Color.Red
                    )
                }
            }

        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        draggableState.offset.roundToInt(),
                        0
                    )
                }
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal
                )
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }

}
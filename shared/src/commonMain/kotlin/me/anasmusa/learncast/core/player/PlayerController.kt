package me.anasmusa.learncast.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import me.anasmusa.learncast.data.model.QueueItem

internal interface PlayerController {
    val currentQueueItemId: MutableStateFlow<Long?>
    val playbackState: MutableStateFlow<Int>

    fun isReady(): Boolean

    fun isEmpty(): Boolean

    fun addFirst(item: QueueItem)

    fun moveToFirst(
        item: QueueItem,
        currentOrder: Int,
    )

    fun replaceFirst(item: QueueItem)

    fun setItems(
        items: List<QueueItem>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean? = null,
    )

    fun playPause()

    fun pause()

    fun getCurrentPositonMs(): Long

    fun seekTo(positionMs: Long)

    fun seek(forward: Boolean)

    fun move(
        from: Int,
        to: Int,
    )

    fun remove(index: Int)

    fun clearQueue(completely: Boolean)

    suspend fun stopService()

    fun restoreService()

    fun destroy()
}

internal expect fun createPlayer(): PlayerController

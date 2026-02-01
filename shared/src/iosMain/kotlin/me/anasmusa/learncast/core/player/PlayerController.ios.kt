package me.anasmusa.learncast.core.player

import kotlinx.coroutines.flow.MutableStateFlow
import me.anasmusa.learncast.data.model.QueueItem

private class IosPlayerController : PlayerController {
    override val currentQueueItemId: MutableStateFlow<Long?>
        get() = MutableStateFlow(0L)
    override val playbackState: MutableStateFlow<Int>
        get() = MutableStateFlow(0)

    override fun isReady(): Boolean = true

    override fun isEmpty(): Boolean = false

    override fun addFirst(item: QueueItem) {
        TODO("Not yet implemented")
    }

    override fun moveToFirst(
        item: QueueItem,
        currentOrder: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun replaceFirst(item: QueueItem) {
        TODO("Not yet implemented")
    }

    override fun setItems(
        items: List<QueueItem>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean?,
    ) {
        TODO("Not yet implemented")
    }

    override fun playPause() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun getCurrentPositonMs(): Long {
        TODO("Not yet implemented")
    }

    override fun seekTo(positionMs: Long) {
        TODO("Not yet implemented")
    }

    override fun seek(forward: Boolean) {
        TODO("Not yet implemented")
    }

    override fun move(
        from: Int,
        to: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun remove(index: Int) {
        TODO("Not yet implemented")
    }

    override fun clearQueue(completely: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun stopService() {
        TODO("Not yet implemented")
    }

    override fun restoreService() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}

internal actual fun createPlayer(): PlayerController = IosPlayerController()

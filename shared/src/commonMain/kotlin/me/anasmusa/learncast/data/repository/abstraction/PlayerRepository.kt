package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.anasmusa.learncast.data.model.QueueItem

interface PlayerRepository {
    val currentQueueItem: StateFlow<QueueItem?>
    val playbackPositionMs: StateFlow<Long>
    val playbackState: StateFlow<Int>
    val events: Channel<Int>

    fun observeQueuedCount(): Flow<Int>

    fun addToQueue(item: QueueItem)

    fun setToQueue(
        items: List<QueueItem>,
        playWhenReady: Boolean? = null,
    )

    fun togglePlayback()

    fun pause()

    fun seekTo(positionMs: Long)

    fun seek(forward: Boolean)

    fun refreshCurrent()

    fun move(
        from: Int,
        to: Int,
    )

    fun removeFromQueue(
        index: Int,
        id: Long,
    )

    suspend fun clearQueue(completely: Boolean)

    suspend fun stopService()

    suspend fun startService(playWhenReady: Boolean?)

    fun destroy()
}

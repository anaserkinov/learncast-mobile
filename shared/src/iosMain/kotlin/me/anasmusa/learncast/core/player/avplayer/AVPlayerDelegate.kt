package me.anasmusa.learncast.core.player.avplayer

import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.network.TokenProvider

interface AVPlayerDelegate {
    companion object {
        lateinit var factory: () -> AVPlayerDelegate
    }

    fun setCallback(callback: AVPlayerCallback)

    fun setTokenProvider(provider: TokenProvider)

    fun setPlayInBackground(value: Boolean)

    fun isPlaying(): Boolean

    fun setPlayWhenReady(value: Boolean)

    fun playWhenReady(): Boolean

    fun playbackState(): PlaybackState

    fun add(
        index: Int,
        item: QueueItem,
    )

    fun move(
        currentItem: QueueItem?,
        currentIndex: Int,
        newIndex: Int,
    )

    fun seekTo(
        itemIndex: Int,
        positionMs: Long,
    )

    fun seekTo(positionMs: Long)

    fun play()

    fun pause()

    fun reload()

    fun currentItem(): QueueItem?

    fun replace(
        index: Int,
        item: QueueItem,
    )

    fun setItem(
        item: QueueItem,
        startPosition: Long,
    )

    fun setItems(
        items: List<QueueItem>,
        startIndex: Int,
        startPosition: Long,
    )

    fun getCurrentPosition(): Long

    fun getDuration(): Long

    fun getCurrentItemIndex(): Int

    fun getItemCount(): Int

    fun seekToNextItem()

    fun removeItem(index: Int)

    fun removeItems(
        from: Int,
        to: Int,
    )

    fun release()
}

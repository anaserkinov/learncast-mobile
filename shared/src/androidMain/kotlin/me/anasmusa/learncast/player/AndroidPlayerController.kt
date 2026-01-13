package me.anasmusa.learncast.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.core.STATE_LOADING
import me.anasmusa.learncast.core.STATE_PAUSED
import me.anasmusa.learncast.core.STATE_PLAYING
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.toMediaItem
import kotlin.math.max
import kotlin.math.min

@UnstableApi
private class AndroidPlayerController(private val context: Context) : PlayerController {

    private var controllerFuture: ListenableFuture<MediaController>? =
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()

    override val currentQueueItemId = MutableStateFlow<Long?>(null)
    override val playbackState = MutableStateFlow(STATE_LOADING)

    private var wasPlaying = false
    private var isStopped = false
    private var lastReturnedPositionMs: Long = 0L

    init {
        addListener(true)
    }

    private fun addListener(initial: Boolean){
        controllerFuture?.addListener({
            controllerFuture?.get()?.let { controller ->
                if (initial){
                    currentQueueItemId.value = controller.currentMediaItem?.mediaId?.toLong()
                    playbackState.value = if (controller.playbackState == Player.STATE_BUFFERING)
                        STATE_LOADING
                    else if (controller.isPlaying)
                        STATE_PLAYING else STATE_PAUSED
                }

                controller.addListener(
                    object : Player.Listener {
                        override fun onEvents(
                            player: Player,
                            events: Player.Events
                        ) {
                            super.onEvents(player, events)
                            if (isStopped) return
                            when {
                                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) -> {
                                    playbackState.value = if (player.playbackState == Player.STATE_BUFFERING) STATE_LOADING
                                    else if (player.isPlaying) STATE_PLAYING
                                    else STATE_PAUSED
                                }

                                events.contains(Player.EVENT_IS_PLAYING_CHANGED) -> {
                                    playbackState.value = if (player.isPlaying) STATE_PLAYING
                                    else STATE_PAUSED
                                }
                            }
                        }

                        /**
                         * onEvents(Player.EVENT_MEDIA_ITEM_TRANSITION) won't be called when the last media item
                         * in playlist removed !!! That's why i'm using the method instead
                         */
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            if (isStopped) return
                            currentQueueItemId.value = mediaItem?.mediaId?.toLong()
                        }
                    }
                )
            }
        }, MoreExecutors.directExecutor())
    }

    override fun isReady(): Boolean = controllerFuture?.isDone ?: false
    override fun isEmpty(): Boolean = controllerFuture?.get()?.mediaItemCount == 0

    override fun addFirst(item: QueueItem) {
        controllerFuture?.get()?.let { controller ->
            controller.playWhenReady = true
            controller.addMediaItem(0, item.toMediaItem(context))
            controller.seekTo(0, item.lastPositionMs?.inWholeMilliseconds ?: 0L)
            controller.prepare()
        }
    }

    override fun moveToFirst(item: QueueItem, currentOrder: Int) {
        controllerFuture?.get()?.let { controller ->
            controller.playWhenReady = true
            controller.moveMediaItem(currentOrder, 0)
            controller.seekTo(0, 0L)
            controller.prepare()
        }
    }

    override fun replaceFirst(item: QueueItem) {
        controllerFuture?.get()?.let { controller ->
            controller.replaceMediaItem(0, item.toMediaItem(context))
            controller.seekTo(0L)
        }
    }

    override fun setItems(
        items: List<QueueItem>,
        startIndex: Int,
        startPositionMs: Long,
        playWhenReady: Boolean?
    ) {
        controllerFuture?.get()?.let { controller ->
            controller.playWhenReady = playWhenReady ?: wasPlaying
            controller.setMediaItems(
                items.map { queueItem -> queueItem.toMediaItem(context) },
                startIndex,
                startPositionMs
            )
            if (controller.playWhenReady)
                controller.prepare()
        }
    }

    override fun playPause() {
        controllerFuture?.get()?.let { controller ->
            if (controller.playbackState == Player.STATE_READY || controller.playbackState == Player.STATE_ENDED){
                if (controller.isPlaying)
                    controller.pause()
                else
                    controller.play()
            } else if (controller.playbackState == Player.STATE_IDLE){
                controller.playWhenReady = true
                controller.prepare()
            }
        }
    }

    override fun pause() {
        controllerFuture?.get()?.pause()
    }

    override fun getCurrentPositonMs(): Long {
        controllerFuture?.get()?.let {
            if (it.currentMediaItem != null)
                lastReturnedPositionMs = it.currentPosition
        }
        return lastReturnedPositionMs
    }

    override fun seekTo(positionMs: Long) {
        controllerFuture?.get()?.seekTo(positionMs)
    }

    override fun seek(forward: Boolean) {
        controllerFuture?.get()?.let {
            if (forward)
                it.seekTo(min(it.currentPosition + 30_000, it.duration))
            else
                it.seekTo(max(it.currentPosition - 10_000, 0))
        }
    }

    override fun move(from: Int, to: Int) {
        controllerFuture?.get()?.let {
            it.moveMediaItem(from, to)
        }
    }

    override fun remove(index: Int) {
        controllerFuture?.get()?.let {
            val currentMediaItemIndex = it.currentMediaItemIndex
            val mediaItemCount = it.mediaItemCount
            if (currentMediaItemIndex == index)
                it.seekToNextMediaItem()
            if (mediaItemCount == 1 || index != currentMediaItemIndex)
                it.removeMediaItem(index)
        }
    }

    override fun clearQueue(completely: Boolean) {
        controllerFuture?.get()?.let {
            val start = if (completely) 0 else 1
            if (it.mediaItemCount > start)
                it.removeMediaItems(start, it.mediaItemCount - 1)
        }
    }

    override suspend fun stopService() {
        isStopped = true
        controllerFuture?.let {
            it.get().let { controller ->
                withContext(Dispatchers.Main){
                    wasPlaying = controller.playbackState == MediaController.STATE_BUFFERING && controller.playWhenReady ||
                            controller.isPlaying
                    controller.playWhenReady = false
                    controller.pause()
                    controller.sendCustomCommand(
                        SessionCommand("destroy_player", Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
                delay(500)
            }
            withContext(Dispatchers.Main){
                it.get().release()
                MediaController.releaseFuture(it)
            }
            controllerFuture = null
        }
    }

    override fun restoreService() {
        isStopped = false
        controllerFuture = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()
        addListener(false)
    }

    override fun destroy() {
        isStopped = false
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

}

internal actual fun createPlayer(): PlayerController {
    return AndroidPlayerController(ApplicationLoader.context)
}
package me.anasmusa.learncast.core.player.avplayer

import me.anasmusa.learncast.data.model.QueueItem

interface AVPlayerCallback {
    fun onPlaybackStateChanged(state: PlaybackState)

    fun onIsPlayingChanged(isPlaying: Boolean)

    fun onItemTransition(
        oldItem: QueueItem?,
        oldPositionMs: Long,
        newItem: QueueItem?,
        reason: ItemTransitionReason,
    )

    fun onPlaybackResumption()
}

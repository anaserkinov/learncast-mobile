package me.anasmusa.learncast.core.player

import kotlinx.coroutines.flow.StateFlow

internal interface AudioPlayer {
    val playbackState: StateFlow<Int>

    fun getCurrentPositonMs(): Long

    fun start(from: Long)

    fun stop()

    fun destroy()
}

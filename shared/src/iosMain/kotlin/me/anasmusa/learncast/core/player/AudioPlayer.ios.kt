package me.anasmusa.learncast.core.player

import kotlinx.coroutines.flow.StateFlow

private class IosAudioPlayer : AudioPlayer {
    override val playbackState: StateFlow<Int>
        get() = TODO("Not yet implemented")

    override fun getCurrentPositonMs(): Long {
        TODO("Not yet implemented")
    }

    override fun start(from: Long) {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }
}

internal actual fun createAudioPlayer(
    audioPath: String,
    startPosition: Long,
): AudioPlayer = IosAudioPlayer()

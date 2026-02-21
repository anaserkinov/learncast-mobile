package me.anasmusa.learncast.core.player.avplayer.cache

data class CacheSpan(
    val key: String,
    val startOffset: Long,
    val endOffset: Long, // Inclusive
    val filePath: String,
    val lastAccessedAt: Long,
) {
    val length: Long
        get() = endOffset - startOffset + 1
}

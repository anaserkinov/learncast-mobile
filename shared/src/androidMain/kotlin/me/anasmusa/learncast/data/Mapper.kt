package me.anasmusa.learncast.data

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem

internal fun QueueItem.toMediaItem(context: Context): MediaItem =
    MediaItem
        .Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(subTitle)
                .setArtist(subTitle)
                .setDescription(description)
                .setArtworkUri(
                    coverImagePath?.normalizeUrl()?.toUri() ?: "android.resource://${context.packageName}/${appConfig.mainLogoInt}".toUri(),
                ).setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        ).setUri(audioPath.normalizeUrl())
        .apply {
            if (startMs != null && endMs != null) {
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration
                        .Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build(),
                )
            }
        }.build()

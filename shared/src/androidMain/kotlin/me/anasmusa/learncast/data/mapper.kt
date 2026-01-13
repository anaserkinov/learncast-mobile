package me.anasmusa.learncast.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.model.QueueItem

internal fun QueueItem.toMediaItem(context: Context): MediaItem{
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setSubtitle(subTitle)
                .setArtist(subTitle)
                .setDescription(description)
                .setArtworkUri(
                    if (coverImagePath != null)
                        Uri.parse(coverImagePath.normalizeUrl())
                    else
                        Uri.parse("android.resource://${context.packageName}/${appConfig.mainLogo}")
                )
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .setUri(audioPath.normalizeUrl())
        .apply {
            if (startMs != null && endMs != null)
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
        }
        .build()
}
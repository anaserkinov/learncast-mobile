package me.anasmusa.learncast.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
class BitmapLoader(
    private val context: Context,
    private val scope: CoroutineScope,
) : BitmapLoader {
    val imageLoader = ImageLoader(context)

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        Futures.immediateFailedFuture(
            UnsupportedOperationException("Bitmap decoding not supported"),
        )

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val settable = SettableFuture.create<Bitmap>()
        scope.launch {
            val bitmap =
                imageLoader
                    .execute(
                        ImageRequest
                            .Builder(context)
                            .data(uri)
                            .allowHardware(false)
                            .size(500)
                            .build(),
                    ).image
                    ?.toBitmap()
            if (bitmap == null) {
                settable.setException(Exception())
            } else {
                settable.set(bitmap)
            }
        }
        return settable
    }
}

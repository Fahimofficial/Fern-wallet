package com.ivy.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.ivy.base.media.AccountMedia
import com.ivy.base.threading.DispatchersProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/** Deletes account media files; a seam so the repository can be tested without files. */
interface AccountMediaFiles {
    suspend fun delete(paths: Collection<String>)
    suspend fun deleteAll()
}

/**
 * Turns a picked image into a small private copy for an account, and deletes copies that are no
 * longer used. Images come from the system Photo Picker, so no storage permission is involved.
 * All work runs on the IO dispatcher.
 */
@Singleton
class AccountMediaStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatchersProvider,
) : AccountMediaFiles {

    /** A square logo, at most [LogoMaxPx] px. @return its path, or null if the image can't be read. */
    suspend fun importLogo(uri: Uri): String? = import(uri, maxSide = LogoMaxPx, square = true)

    /** A card background, at most [BackgroundMaxPx] px on its long side. */
    suspend fun importBackground(uri: Uri): String? = import(uri, maxSide = BackgroundMaxPx, square = false)

    override suspend fun delete(paths: Collection<String>) {
        withContext(dispatchers.io) {
            paths.mapNotNull { AccountMedia.ownedFile(context, it) }.forEach { it.delete() }
        }
    }

    override suspend fun deleteAll() {
        withContext(dispatchers.io) { AccountMedia.dir(context).deleteRecursively() }
    }

    private suspend fun import(uri: Uri, maxSide: Int, square: Boolean): String? = withContext(dispatchers.io) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Decode already shrunk (never a full 12 MP photo in memory), and in software so
                // the bitmap can be cropped and compressed.
                val (width, height) = info.size.width to info.size.height
                val side = if (square) min(width, height) else max(width, height)
                decoder.setTargetSampleSize(max(1, side / maxSide))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val output = fit(decoded, maxSide, square)
            val dir = AccountMedia.dir(context).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.${if (square) "png" else "jpg"}")
            file.outputStream().use { stream ->
                if (square) {
                    output.compress(Bitmap.CompressFormat.PNG, PngQuality, stream)
                } else {
                    output.compress(Bitmap.CompressFormat.JPEG, JpegQuality, stream)
                }
            }
            file.absolutePath
        } catch (e: IOException) {
            Timber.w(e, "Couldn't import account image")
            null
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Couldn't import account image")
            null
        }
    }

    private companion object {
        const val LogoMaxPx = 256
        const val BackgroundMaxPx = 1080
        const val JpegQuality = 85
        const val PngQuality = 100
    }
}

/** Centre-crops to a square when asked, then scales down so the long side is at most [maxSide]. */
internal fun fit(bitmap: Bitmap, maxSide: Int, square: Boolean): Bitmap {
    val cropped = if (square) {
        val side = min(bitmap.width, bitmap.height)
        Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
    } else {
        bitmap
    }
    val longSide = max(cropped.width, cropped.height)
    if (longSide <= maxSide) return cropped
    val scale = maxSide.toFloat() / longSide
    return Bitmap.createScaledBitmap(
        cropped,
        max(1, (cropped.width * scale).toInt()),
        max(1, (cropped.height * scale).toInt()),
        true,
    )
}

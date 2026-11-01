package com.ivy.settings.support

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.ivy.base.links.SupportDestinations
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * Saving and sharing the UPI QR code as a PNG.
 *
 * Saving goes through the system file picker (no storage permission). Sharing writes the PNG to
 * the app cache and hands a read-only content:// URI to the app the user picks.
 */
internal object SupportQrExport {
    const val FILE_NAME = "SimplyFahim-UPI-QR.png"
    const val MIME_TYPE = "image/png"

    /** Must match the FileProvider declared in the app manifest. */
    private const val AUTHORITY_SUFFIX = ".supportshare"
    private const val SHARE_DIR = "support_share"
    private const val PNG_QUALITY = 100
    private const val EXPORT_FAILURE_MESSAGE = "Couldn't share the UPI QR code"

    private fun renderPng(out: OutputStream) {
        val matrix = UpiQrCode.encode(SupportDestinations.upiPaymentUri())
        val bitmap = UpiQrCode.toBitmap(matrix, UpiQrCode.EXPORT_PIXELS_PER_MODULE)
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) { "PNG encoding failed" }
        } finally {
            bitmap.recycle()
        }
    }

    /** Writes the QR PNG to [uri] (from ACTION_CREATE_DOCUMENT). Returns false on failure. */
    fun save(context: Context, uri: Uri): Boolean = try {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
        stream.use(::renderPng)
        true
    } catch (e: IOException) {
        Timber.w(e, "Couldn't save the UPI QR code")
        false
    } catch (e: IllegalStateException) {
        Timber.w(e, "Couldn't save the UPI QR code")
        false
    } catch (e: SecurityException) {
        Timber.w(e, "Couldn't save the UPI QR code")
        false
    }

    /** Opens the system share sheet with the QR PNG. Returns false if nothing could be shared. */
    fun share(context: Context, chooserTitle: String, text: String): Boolean = try {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        file.outputStream().use(::renderPng)
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newRawUri(FILE_NAME, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
        true
    } catch (e: IOException) {
        Timber.w(e, EXPORT_FAILURE_MESSAGE)
        false
    } catch (e: IllegalArgumentException) {
        Timber.w(e, EXPORT_FAILURE_MESSAGE)
        false
    } catch (e: IllegalStateException) {
        Timber.w(e, EXPORT_FAILURE_MESSAGE)
        false
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, EXPORT_FAILURE_MESSAGE)
        false
    }
}

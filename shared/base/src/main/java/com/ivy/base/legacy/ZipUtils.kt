package com.ivy.base.legacy

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val MODE_WRITE = "w"
private const val MODE_READ = "r"

fun zip(context: Context, zipFile: Uri, files: List<File>) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_WRITE).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(it))).use { outStream ->
                zip(outStream, files)
            }
        }
    }
}

internal fun zip(
    outStream: ZipOutputStream,
    files: List<File>,
    includeParentFolder: Boolean = false
) {
    files.forEach { file ->
        if (file.isDirectory) {
            file.mkdir()
            zip(outStream, file.listFiles()?.toList() ?: emptyList(), includeParentFolder = true)
        } else {
            // Always a relative name ("folder/file"); unzip() rejects absolute entry names.
            val parentName = file.parentFile?.name
            val fileLoc: String =
                if (parentName.isNullOrEmpty() || !includeParentFolder) {
                    file.name
                } else {
                    "$parentName/${file.name}"
                }

            outStream.putNextEntry(ZipEntry(fileLoc))
            BufferedInputStream(FileInputStream(file)).use { inStream ->
                inStream.copyTo(outStream)
            }
        }
    }
}

fun unzip(context: Context, zipFile: Uri, location: File) {
    context.contentResolver.openFileDescriptor(zipFile, MODE_READ).use { descriptor ->
        descriptor?.fileDescriptor?.let {
            unzip(FileInputStream(it), location)
        }
    }
}

fun unzip(fileInputStream: FileInputStream, location: File) {
    ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
        if (location.exists() && !location.isDirectory) {
            throw IllegalStateException("Location file must be directory or not exist")
        }

        if (!location.isDirectory) location.mkdirs()

        val destination = location.canonicalFile

        while (true) {
            val zipEntry: ZipEntry = zipInputStream.nextEntry ?: break
            val unzipFile = resolveZipEntry(destination, zipEntry.name)
            if (zipEntry.isDirectory) {
                if (!unzipFile.isDirectory) unzipFile.mkdirs()
            } else {
                val unzipParentDir = unzipFile.parentFile
                if (unzipParentDir != null && !unzipParentDir.isDirectory) {
                    unzipParentDir.mkdirs()
                }
                BufferedOutputStream(FileOutputStream(unzipFile)).use { outStream ->
                    zipInputStream.copyTo(outStream)
                }
            }
        }
    }
}

private val windowsDrivePrefix = Regex("^[A-Za-z]:")

/**
 * Resolves a zip entry name inside [destination] and rejects names that are absolute or would
 * escape it ("Zip Slip"), e.g. `../../shared_prefs/x.xml` or `/data/data/<app>/databases/x.db`.
 * Backups are user-picked files, so a crafted archive must not be able to overwrite the app's
 * database or preferences. Archives written by the app only ever contain relative names.
 */
internal fun resolveZipEntry(destination: File, entryName: String): File {
    if (entryName.startsWith("/") || entryName.startsWith("\\") || windowsDrivePrefix.containsMatchIn(entryName)) {
        throw ZipException("Zip entry has an absolute path: $entryName")
    }
    val resolved = File(destination, entryName).canonicalFile
    if (!resolved.toPath().startsWith(destination.toPath())) {
        throw ZipException("Zip entry is outside of the target directory: $entryName")
    }
    return resolved
}

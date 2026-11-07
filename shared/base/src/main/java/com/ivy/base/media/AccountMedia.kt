package com.ivy.base.media

import android.content.Context
import java.io.File

/**
 * Where account logos and background images live: app-private storage, nothing shared.
 *
 * Paths reach the app from the database and from backups, which may come from another device or
 * be tampered with, so every path is checked to really be inside this folder before it is read or
 * deleted: a crafted backup can't make the app delete or display arbitrary app files.
 */
object AccountMedia {
    private const val DirName = "account_media"

    fun dir(context: Context): File = File(context.filesDir, DirName)

    /** [path] as a file inside [dir], or null when it points anywhere else. */
    fun ownedFile(context: Context, path: String?): File? = ownedFile(dir(context), path)

    /** Testable core of [ownedFile]. */
    fun ownedFile(dir: File, path: String?): File? {
        if (path.isNullOrBlank()) return null
        val root = dir.canonicalFile
        val file = File(path).canonicalFile
        return file.takeIf { it.parentFile == root }
    }
}

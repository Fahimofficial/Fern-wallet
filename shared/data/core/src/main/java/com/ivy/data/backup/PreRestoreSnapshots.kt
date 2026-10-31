package com.ivy.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies of the wallet taken automatically right before a backup is imported, so an import can
 * be undone. Stored in app-private storage (not in the cache, which the system may clear), and
 * only the most recent [KEEP] are kept.
 */
@Singleton
class PreRestoreSnapshots internal constructor(
    private val directory: () -> File,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({ File(context.filesDir, DIRECTORY) })

    fun save(json: String, timestampMillis: Long) {
        val dir = directory().apply { mkdirs() }
        val target = File(dir, "$PREFIX$timestampMillis.zip")
        val partial = File(dir, "${target.name}.partial")
        FileOutputStream(partial).use { BackupArchive.writeZip(it, json) }
        if (!partial.renameTo(target)) {
            partial.delete()
            error("Couldn't save the pre-restore copy")
        }
        all().drop(KEEP).forEach { it.delete() }
    }

    /** The newest snapshot, or null if there is none. */
    fun latest(): File? = all().firstOrNull()

    fun delete(snapshot: File) {
        snapshot.delete()
    }

    private fun all(): List<File> = directory()
        .listFiles { file -> file.isFile && file.name.startsWith(PREFIX) && file.name.endsWith(".zip") }
        .orEmpty()
        .sortedByDescending { it.name.removePrefix(PREFIX).removeSuffix(".zip").toLongOrNull() ?: 0L }

    companion object {
        private const val DIRECTORY = "pre-restore"
        private const val PREFIX = "pre-restore-"
        const val KEEP = 3
    }
}

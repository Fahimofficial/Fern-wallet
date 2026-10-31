package com.ivy.data.backup.auto

import com.ivy.base.threading.DispatchersProvider
import com.ivy.base.time.TimeProvider
import com.ivy.data.backup.BackupCrypto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Writes a complete backup (see `BackupDataUseCase`). */
interface BackupWriter {
    suspend fun writeBackup(output: OutputStream, key: BackupCrypto.Key?)
}

/**
 * Automatic backups to a folder the user picked (any Storage Access Framework location: phone
 * storage, SD card, or a synced cloud folder). Each run writes a new, timestamped file and keeps
 * the newest [KEEP]; nothing ever leaves the device unless the chosen folder is synced.
 */
@Singleton
class AutoBackupUseCase @Inject constructor(
    private val preferences: AutoBackupPreferences,
    private val folderAccess: BackupFolderAccess,
    private val keyWrapper: BackupKeyWrapper,
    private val backupWriter: BackupWriter,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatchersProvider,
) {
    /** Makes a backup now. Returns null on success, otherwise why it failed (also saved for the UI). */
    suspend fun backUpNow(): AutoBackupFailure? = withContext(dispatchers.io) {
        val failure = runBackup()
        if (failure == null) {
            preferences.lastSuccessMillis = timeProvider.utcNow().toEpochMilli()
        }
        preferences.lastFailure = failure
        failure
    }

    /** Sets the folder (null to forget it). Takes over the access the folder picker granted. */
    fun setFolder(treeUri: String?) {
        val previous = preferences.folderUri
        if (previous != null && previous != treeUri) folderAccess.releaseAccess(previous)
        if (treeUri != null) folderAccess.keepAccess(treeUri)
        preferences.folderUri = treeUri
        preferences.lastFailure = null
    }

    fun folderName(): String? = preferences.folderUri?.let(folderAccess::displayName)

    /**
     * Protects future automatic backups with [password] (null or empty: no password). Slow on
     * purpose (key derivation); call it off the main thread.
     */
    suspend fun setPassword(password: CharArray?): Unit = withContext(dispatchers.io) {
        preferences.key = password?.takeIf { it.isNotEmpty() }?.let {
            val key = BackupCrypto.newKey(it)
            StoredBackupKey(
                salt = key.salt,
                iterations = key.iterations,
                wrappedKey = keyWrapper.wrap(key.secret.encoded),
            )
        }
        preferences.lastFailure = null
    }

    fun isPasswordProtected(): Boolean = preferences.key != null

    @Suppress("ReturnCount")
    private suspend fun runBackup(): AutoBackupFailure? {
        val folder = preferences.folderUri?.let(folderAccess::open)
            ?: return AutoBackupFailure.FOLDER_UNAVAILABLE
        val key = preferences.key?.let { stored ->
            loadKey(stored) ?: return AutoBackupFailure.KEY_UNAVAILABLE
        }
        val name = fileName(encrypted = key != null)
        val mimeType = if (key != null) MIME_ENCRYPTED else MIME_ZIP
        return try {
            val output = folder.create(name, mimeType) ?: return AutoBackupFailure.FOLDER_UNAVAILABLE
            output.use { backupWriter.writeBackup(it, key) }
            prune(folder)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.e(e, "Automatic backup failed")
            folder.files().firstOrNull { it.name == name }?.let(folder::delete)
            AutoBackupFailure.WRITE_FAILED
        } catch (e: SecurityException) {
            Timber.e(e, "Lost access to the backup folder")
            AutoBackupFailure.FOLDER_UNAVAILABLE
        }
    }

    private fun loadKey(stored: StoredBackupKey): BackupCrypto.Key? = try {
        BackupCrypto.keyFromRaw(stored.salt, stored.iterations, keyWrapper.unwrap(stored.wrappedKey))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Keystore errors come as many exception types; any of them means the key is gone.
        Timber.e(e, "Couldn't unwrap the automatic backup key")
        null
    }

    private fun prune(folder: BackupFolder) {
        folder.files()
            .filter { it.name.startsWith(PREFIX) }
            .sortedByDescending { it.name.removePrefix(PREFIX) }
            .drop(KEEP)
            .forEach { folder.delete(it) }
    }

    private fun fileName(encrypted: Boolean): String {
        val stamp = FILE_TIME.format(timeProvider.utcNow().atZone(ZoneOffset.UTC))
        return PREFIX + stamp + if (encrypted) ENCRYPTED_EXTENSION else ZIP_EXTENSION
    }

    companion object {
        /** Automatic backups kept in the folder; older ones are deleted. */
        const val KEEP = 10
        const val PREFIX = "FernWallet-auto-"
        const val ZIP_EXTENSION = ".zip"
        const val ENCRYPTED_EXTENSION = ".fernbackup"
        private const val MIME_ZIP = "application/zip"
        private const val MIME_ENCRYPTED = "application/octet-stream"

        // UTC and sortable, so the newest file has the greatest name.
        private val FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss'Z'")
    }
}

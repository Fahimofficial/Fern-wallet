package com.ivy.data.backup.auto

import com.ivy.base.legacy.SharedPrefs
import java.util.Base64
import javax.inject.Inject

enum class AutoBackupFrequency { OFF, DAILY, WEEKLY }

/** Why the last automatic backup didn't happen. */
enum class AutoBackupFailure {
    /** No folder chosen, or the app lost access to it (folder deleted, permission revoked). */
    FOLDER_UNAVAILABLE,

    /** The backup password key can't be used anymore (e.g. the phone's keystore was reset). */
    KEY_UNAVAILABLE,

    /** Creating or writing the file failed (e.g. the storage is full). */
    WRITE_FAILED,
}

/** The backup key, wrapped by the Android Keystore, plus what's needed to re-derive it. */
class StoredBackupKey(val salt: ByteArray, val iterations: Int, val wrappedKey: ByteArray)

/** Automatic-backup settings and status, kept with the app's other preferences. */
class AutoBackupPreferences @Inject constructor(
    private val sharedPrefs: SharedPrefs,
) {
    var folderUri: String?
        get() = sharedPrefs.getString(FOLDER_URI, null)
        set(value) = sharedPrefs.putString(FOLDER_URI, value)

    var frequency: AutoBackupFrequency
        get() = sharedPrefs.getString(FREQUENCY, null)
            ?.let { name -> AutoBackupFrequency.entries.firstOrNull { it.name == name } }
            ?: AutoBackupFrequency.OFF
        set(value) = sharedPrefs.putString(FREQUENCY, value.name)

    var lastSuccessMillis: Long?
        get() = sharedPrefs.getLong(LAST_SUCCESS, NONE).takeIf { it != NONE }
        set(value) = sharedPrefs.putLong(LAST_SUCCESS, value ?: NONE)

    var lastFailure: AutoBackupFailure?
        get() = sharedPrefs.getString(LAST_FAILURE, null)
            ?.let { name -> AutoBackupFailure.entries.firstOrNull { it.name == name } }
        set(value) = sharedPrefs.putString(LAST_FAILURE, value?.name)

    var key: StoredBackupKey?
        get() {
            val salt = sharedPrefs.getString(KEY_SALT, null) ?: return null
            val wrapped = sharedPrefs.getString(KEY_WRAPPED, null) ?: return null
            return StoredBackupKey(
                salt = decode(salt),
                iterations = sharedPrefs.getInt(KEY_ITERATIONS, 0),
                wrappedKey = decode(wrapped),
            )
        }
        set(value) {
            sharedPrefs.putString(KEY_SALT, value?.salt?.let(::encode))
            sharedPrefs.putInt(KEY_ITERATIONS, value?.iterations ?: 0)
            sharedPrefs.putString(KEY_WRAPPED, value?.wrappedKey?.let(::encode))
        }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val FOLDER_URI = "auto_backup_folder_uri"
        const val FREQUENCY = "auto_backup_frequency"
        const val LAST_SUCCESS = "auto_backup_last_success"
        const val LAST_FAILURE = "auto_backup_last_failure"
        const val KEY_SALT = "auto_backup_key_salt"
        const val KEY_ITERATIONS = "auto_backup_key_iterations"
        const val KEY_WRAPPED = "auto_backup_key_wrapped"
        const val NONE = -1L
    }
}

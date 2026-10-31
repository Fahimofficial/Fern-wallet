package com.ivy.data.backup.auto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * Protects the automatic-backup key at rest. Automatic backups run without the user, so the
 * key derived from the backup password has to be kept; it is only ever stored wrapped.
 */
interface BackupKeyWrapper {
    fun wrap(raw: ByteArray): ByteArray

    /** Throws if the key can't be unwrapped (e.g. the keystore entry is gone). */
    fun unwrap(wrapped: ByteArray): ByteArray
}

/**
 * Wraps with a non-exportable AES-GCM key in the Android Keystore, so the wrapped bytes are
 * useless outside this app on this device (they are also excluded from Android backups, which
 * the app disables).
 */
class AndroidKeystoreBackupKeyWrapper @Inject constructor() : BackupKeyWrapper {
    override fun wrap(raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        return cipher.iv + cipher.doFinal(raw)
    }

    override fun unwrap(wrapped: ByteArray): ByteArray {
        require(wrapped.size > IV_BYTES) { "Wrapped key too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            existingKey() ?: error("Keystore key missing"),
            // The IV stored by wrap(), which the Keystore generates randomly for each encryption.
            GCMParameterSpec(TAG_BITS, wrapped, 0, IV_BYTES) // nosemgrep
        )
        return cipher.doFinal(wrapped, IV_BYTES, wrapped.size - IV_BYTES)
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun keystoreKey(): SecretKey = existingKey() ?: KeyGenerator
        .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        .apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build()
            )
        }
        .generateKey()

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "fern_auto_backup_key_wrapper"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}

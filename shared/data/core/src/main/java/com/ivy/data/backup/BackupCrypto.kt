package com.ivy.data.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password protection for backup files.
 *
 * Format: `FERNBAK1` (8 bytes) · salt (16) · PBKDF2 iterations (4, big-endian) · IV (12) ·
 * AES-256-GCM ciphertext of the backup ZIP, including the 16-byte tag. The whole header is
 * authenticated as associated data, so changing any byte of the file makes decryption fail.
 * The key is PBKDF2-HMAC-SHA256 of the password; the password itself is never stored.
 */
object BackupCrypto {
    const val DEFAULT_ITERATIONS = 600_000
    private const val MIN_ITERATIONS = 100_000
    private const val MAX_ITERATIONS = 10_000_000

    private val MAGIC = "FERNBAK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val ITERATIONS_BYTES = 4
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private val HEADER_BYTES = MAGIC.size + SALT_BYTES + ITERATIONS_BYTES + IV_BYTES

    /** Number of leading bytes [isEncrypted] needs. */
    val MAGIC_BYTES: Int = MAGIC.size

    /** A derived key and the parameters needed to derive it again from the password. */
    class Key(val salt: ByteArray, val iterations: Int, val secret: SecretKey)

    fun isEncrypted(header: ByteArray): Boolean =
        header.size >= MAGIC.size && header.copyOf(MAGIC.size).contentEquals(MAGIC)

    fun newKey(
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): Key {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        return deriveKey(password, salt, iterations)
    }

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): Key {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Key(salt.copyOf(), iterations, SecretKeySpec(raw, "AES"))
        } finally {
            spec.clearPassword()
        }
    }

    /** Rebuilds a key from its raw bytes (e.g. after unwrapping it from the Android Keystore). */
    fun keyFromRaw(salt: ByteArray, iterations: Int, raw: ByteArray): Key =
        Key(salt.copyOf(), iterations, SecretKeySpec(raw, "AES"))

    fun encrypt(plain: ByteArray, key: Key, random: SecureRandom = SecureRandom()): ByteArray {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .put(key.salt)
            .putInt(key.iterations)
            .put(iv)
            .array()
        // A fresh random IV for every encryption (above), so a key never reuses a nonce.
        val cipher = Cipher.getInstance("AES/GCM/NoPadding") // nosemgrep
        cipher.init(Cipher.ENCRYPT_MODE, key.secret, GCMParameterSpec(TAG_BITS, iv)) // nosemgrep
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    /** Throws [BackupImportException] with WRONG_PASSWORD or CORRUPTED. */
    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        if (data.size <= HEADER_BYTES || !isEncrypted(data)) corrupted()
        val buffer = ByteBuffer.wrap(data)
        buffer.position(MAGIC.size)
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iterations = buffer.getInt()
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        if (iterations !in MIN_ITERATIONS..MAX_ITERATIONS) corrupted()

        val key = deriveKey(password, salt, iterations)
        return try {
            // Decrypts with the IV stored in the file; IV reuse only matters when encrypting.
            val cipher = Cipher.getInstance("AES/GCM/NoPadding") // nosemgrep
            cipher.init(Cipher.DECRYPT_MODE, key.secret, GCMParameterSpec(TAG_BITS, iv)) // nosemgrep
            cipher.updateAAD(data, 0, HEADER_BYTES)
            cipher.doFinal(data, HEADER_BYTES, data.size - HEADER_BYTES)
        } catch (e: AEADBadTagException) {
            // A wrong password and a modified file look the same; the password is far more likely.
            throw BackupImportException(ImportFailure.WRONG_PASSWORD, e)
        } catch (e: GeneralSecurityException) {
            throw BackupImportException(ImportFailure.CORRUPTED, e)
        }
    }

    private fun corrupted(): Nothing = throw BackupImportException(ImportFailure.CORRUPTED)
}

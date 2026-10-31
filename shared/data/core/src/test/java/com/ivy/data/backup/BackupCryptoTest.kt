package com.ivy.data.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.ByteArrayOutputStream

class BackupCryptoTest {
    // Fewer iterations than in the app keep the tests fast; the format stores the count.
    private val iterations = 100_000
    private val password = "correct horse battery staple".toCharArray()
    private val json = """{"accounts":[],"note":"₹ ünïcødé"}"""

    private fun encryptedBackup(): ByteArray = ByteArrayOutputStream().also {
        BackupArchive.writeBackup(it, json, BackupCrypto.newKey(password, iterations))
    }.toByteArray()

    @Test
    fun `round trip with the right password`() {
        BackupArchive.readJson(encryptedBackup().inputStream(), password = password) shouldBe json
    }

    @Test
    fun `the file doesn't contain the data in clear text`() {
        val bytes = encryptedBackup()
        BackupCrypto.isEncrypted(bytes) shouldBe true
        String(bytes, Charsets.ISO_8859_1).contains("accounts") shouldBe false
    }

    @Test
    fun `a protected backup without a password asks for one`() {
        shouldThrow<BackupImportException> { BackupArchive.readJson(encryptedBackup().inputStream()) }
            .failure shouldBe ImportFailure.PASSWORD_REQUIRED
    }

    @Test
    fun `a wrong password is reported as such`() {
        shouldThrow<BackupImportException> {
            BackupArchive.readJson(encryptedBackup().inputStream(), password = "wrong".toCharArray())
        }.failure shouldBe ImportFailure.WRONG_PASSWORD
    }

    @Test
    fun `changing any byte, including the header, makes decryption fail`() {
        val original = encryptedBackup()
        listOf(10, 30, original.size - 1).forEach { index ->
            val tampered = original.copyOf().also { it[index] = (it[index] + 1).toByte() }
            shouldThrow<BackupImportException> { BackupCrypto.decrypt(tampered, password) }
        }
    }

    @Test
    fun `a truncated file is corrupted`() {
        shouldThrow<BackupImportException> { BackupCrypto.decrypt(encryptedBackup().copyOf(20), password) }
            .failure shouldBe ImportFailure.CORRUPTED
    }

    @Test
    fun `a key rebuilt from its raw bytes decrypts with the password`() {
        val key = BackupCrypto.newKey(password, iterations)
        val rebuilt = BackupCrypto.keyFromRaw(key.salt, key.iterations, key.secret.encoded)
        val encrypted = BackupCrypto.encrypt(json.toByteArray(), rebuilt)

        String(BackupCrypto.decrypt(encrypted, password)) shouldBe json
    }

    @Test
    fun `unprotected backups ignore a password`() {
        val zip = ByteArrayOutputStream().also { BackupArchive.writeBackup(it, json, key = null) }.toByteArray()

        BackupArchive.readJson(zip.inputStream(), password = password) shouldBe json
    }
}

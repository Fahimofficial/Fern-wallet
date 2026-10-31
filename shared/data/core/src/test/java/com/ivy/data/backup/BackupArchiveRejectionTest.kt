package com.ivy.data.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Files that must be refused without loading them fully. */
class BackupArchiveRejectionTest {
    private val json = BackupTestJson

    private fun failureOf(bytes: ByteArray, max: Long = BackupArchive.MAX_JSON_BYTES): ImportFailure =
        shouldThrow<BackupImportException> { readBackup(bytes, max) }.failure

    @Test
    fun `a zip with two JSON files is not a backup`() {
        val bytes = json.toByteArray(Charsets.UTF_16)
        failureOf(zipOf("a.json" to bytes, "b.json" to bytes)) shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `a zip without JSON is not a backup`() {
        failureOf(zipOf("photo.jpg" to byteArrayOf(1, 2, 3))) shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `an empty file is not a backup`() {
        failureOf(ByteArray(0)) shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `a file that isn't JSON is not a backup`() {
        failureOf("hello, world".toByteArray()) shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `entries larger than the limit are refused (zip bomb)`() {
        // 1 MB of zeros compresses to ~1 KB.
        val huge = ByteArray(1024 * 1024)
        failureOf(zipOf("data.json" to huge), max = 64 * 1024) shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `large non-JSON entries are refused instead of being decompressed in full`() {
        val huge = ByteArray(1024 * 1024)
        val zip = zipOf("padding.bin" to huge, "data.json" to json.toByteArray(Charsets.UTF_16))

        failureOf(zip, max = 64 * 1024) shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `the limit applies to all entries together`() {
        // Each entry fits the limit on its own; together they don't.
        val part = ByteArray(40 * 1024)
        val zip = zipOf("a.bin" to part, "b.bin" to part, "data.json" to json.toByteArray(Charsets.UTF_16))

        failureOf(zip, max = 64 * 1024) shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `bare files larger than the limit are refused`() {
        val big = ("{" + " ".repeat(10_000) + "}").toByteArray()
        failureOf(big, max = 1_000) shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `a truncated zip is corrupted`() {
        val zip = ByteArrayOutputStream().also { BackupArchive.writeZip(it, json.repeat(200)) }.toByteArray()
        failureOf(zip.copyOf(zip.size / 2)) shouldBe ImportFailure.CORRUPTED
    }
}

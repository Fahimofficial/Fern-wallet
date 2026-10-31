package com.ivy.data.backup

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = """{"accounts":[],"note":"ünïcødé ₹"}"""

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun read(bytes: ByteArray, max: Long = BackupArchive.MAX_JSON_BYTES) =
        BackupArchive.readJson(ByteArrayInputStream(bytes), max)

    @Test
    fun `round trip through writeZip`() {
        val out = ByteArrayOutputStream()
        BackupArchive.writeZip(out, json)

        read(out.toByteArray()) shouldBe json
    }

    @Test
    fun `reads zips made by older versions (temp file name, UTF-16)`() {
        read(zipOf("data8127364.json" to json.toByteArray(Charsets.UTF_16))) shouldBe json
    }

    @Test
    fun `reads a bare UTF-16 JSON file`() {
        read(json.toByteArray(Charsets.UTF_16)) shouldBe json
    }

    @Test
    fun `reads a bare UTF-8 JSON file`() {
        read(json.toByteArray(Charsets.UTF_8)) shouldBe json
    }

    @Test
    fun `entry names with path traversal are read in memory and never written to disk`() {
        val before = tempFolder.root.parentFile!!.list()!!.toSet()

        read(zipOf("../../shared_prefs/evil.json" to json.toByteArray(Charsets.UTF_16))) shouldBe json

        tempFolder.root.parentFile!!.list()!!.toSet() shouldBe before
    }

    @Test
    fun `ignores directories and non-JSON entries`() {
        read(
            zipOf(
                "folder/" to ByteArray(0),
                "readme.txt" to "hi".toByteArray(),
                "data.json" to json.toByteArray(Charsets.UTF_16),
            )
        ) shouldBe json
    }

    @Test
    fun `a zip with two JSON files is not a backup`() {
        val bytes = json.toByteArray(Charsets.UTF_16)
        shouldThrow<BackupImportException> {
            read(zipOf("a.json" to bytes, "b.json" to bytes))
        }.failure shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `a zip without JSON is not a backup`() {
        shouldThrow<BackupImportException> {
            read(zipOf("photo.jpg" to byteArrayOf(1, 2, 3)))
        }.failure shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `an empty file is not a backup`() {
        shouldThrow<BackupImportException> { read(ByteArray(0)) }
            .failure shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `a file that isn't JSON is not a backup`() {
        shouldThrow<BackupImportException> { read("hello, world".toByteArray()) }
            .failure shouldBe ImportFailure.NOT_A_BACKUP
    }

    @Test
    fun `entries larger than the limit are refused (zip bomb)`() {
        // 1 MB of zeros compresses to ~1 KB.
        val huge = ByteArray(1024 * 1024)
        shouldThrow<BackupImportException> {
            read(zipOf("data.json" to huge), max = 64 * 1024)
        }.failure shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `bare files larger than the limit are refused`() {
        val big = ("{" + " ".repeat(10_000) + "}").toByteArray()
        shouldThrow<BackupImportException> { read(big, max = 1_000) }
            .failure shouldBe ImportFailure.TOO_LARGE
    }

    @Test
    fun `a truncated zip is corrupted`() {
        val zip = ByteArrayOutputStream().also { BackupArchive.writeZip(it, json.repeat(200)) }.toByteArray()
        shouldThrow<BackupImportException> { read(zip.copyOf(zip.size / 2)) }
            .failure shouldBe ImportFailure.CORRUPTED
    }
}

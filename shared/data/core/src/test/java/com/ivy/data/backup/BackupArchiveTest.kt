package com.ivy.data.backup

import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/** Backups that must be read. Invalid input is covered by [BackupArchiveRejectionTest]. */
class BackupArchiveTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = BackupTestJson

    @Test
    fun `round trip through writeZip`() {
        val out = ByteArrayOutputStream()
        BackupArchive.writeZip(out, json)

        readBackup(out.toByteArray()) shouldBe json
    }

    @Test
    fun `reads zips made by older versions (temp file name, UTF-16)`() {
        readBackup(zipOf("data8127364.json" to json.toByteArray(Charsets.UTF_16))) shouldBe json
    }

    @Test
    fun `reads a bare UTF-16 JSON file`() {
        readBackup(json.toByteArray(Charsets.UTF_16)) shouldBe json
    }

    @Test
    fun `reads a bare UTF-8 JSON file`() {
        readBackup(json.toByteArray(Charsets.UTF_8)) shouldBe json
    }

    @Test
    fun `entry names with path traversal are read in memory and never written to disk`() {
        val name = "fern-zip-slip-${UUID.randomUUID()}.json"
        val traversal = "../../$name"
        val wouldBeWritten = listOf(
            File(tempFolder.root, traversal).canonicalFile,
            File(System.getProperty("user.dir"), traversal).canonicalFile,
            File(System.getProperty("java.io.tmpdir"), name),
        )

        readBackup(zipOf(traversal to json.toByteArray(Charsets.UTF_16))) shouldBe json

        wouldBeWritten.forEach { it.exists() shouldBe false }
    }

    @Test
    fun `ignores directories and non-JSON entries`() {
        readBackup(
            zipOf(
                "folder/" to ByteArray(0),
                "readme.txt" to "hi".toByteArray(),
                "data.json" to json.toByteArray(Charsets.UTF_16),
            )
        ) shouldBe json
    }
}

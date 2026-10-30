package com.ivy.base.legacy

import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream

class ZipUtilsTest {

    private lateinit var workDir: File
    private lateinit var destination: File

    @Before
    fun setUp() {
        workDir = Files.createTempDirectory("zip-utils-test").toFile()
        destination = File(workDir, "cache/backup123")
    }

    @After
    fun tearDown() {
        workDir.deleteRecursively()
    }

    @Test
    fun `extracts regular entries, including nested folders`() {
        val zip = zipOf(BACKUP_JSON to EMPTY_JSON, "nested/dir/file.txt" to "hello")

        unzip(FileInputStream(zip), destination)

        File(destination, BACKUP_JSON).readText() shouldBe EMPTY_JSON
        File(destination, "nested/dir/file.txt").readText() shouldBe "hello"
    }

    @Test
    fun `rejects an entry that climbs out of the destination`() {
        val victim = File(workDir, "shared_prefs/ivy_wallet_prefs.xml")
        val zip = zipOf(BACKUP_JSON to EMPTY_JSON, "../../shared_prefs/ivy_wallet_prefs.xml" to "<map/>")

        assertThrows(ZipException::class.java) {
            unzip(FileInputStream(zip), destination)
        }

        victim.exists() shouldBe false
    }

    @Test
    fun `rejects an absolute entry name`() {
        val victim = File(workDir, "absolute.txt")
        val zip = zipOf(victim.absolutePath to "x")

        assertThrows(ZipException::class.java) {
            unzip(FileInputStream(zip), destination)
        }

        victim.exists() shouldBe false
        File(destination, victim.absolutePath).exists() shouldBe false
    }

    @Test
    fun `rejects Windows-style absolute entry names`() {
        listOf("C:\\Users\\x.txt", "\\\\server\\share\\x.txt").forEach { name ->
            assertThrows(ZipException::class.java) {
                resolveZipEntry(destination.canonicalFile, name)
            }
        }
    }

    @Test
    fun `round-trips an archive written by zip, including a nested folder`() {
        val source = File(workDir, "source").apply { mkdirs() }
        val json = File(source, BACKUP_JSON).apply { writeText(EMPTY_JSON) }
        val folder = File(source, "attachments").apply { mkdirs() }
        File(folder, "note.txt").writeText("hello")
        val zip = File(workDir, "roundtrip.zip")

        ZipOutputStream(zip.outputStream()).use { zip(it, listOf(json, folder)) }
        unzip(FileInputStream(zip), destination)

        File(destination, BACKUP_JSON).readText() shouldBe EMPTY_JSON
        File(destination, "attachments/note.txt").readText() shouldBe "hello"
    }

    @Test
    fun `rejects a sibling directory that shares the destination prefix`() {
        val zip = zipOf("../backup123-evil/file.txt" to "x")

        assertThrows(ZipException::class.java) {
            unzip(FileInputStream(zip), destination)
        }

        File(workDir, "cache/backup123-evil/file.txt").exists() shouldBe false
    }

    private fun zipOf(vararg entries: Pair<String, String>): File {
        val file = File(workDir, "input-${System.nanoTime()}.zip")
        ZipOutputStream(file.outputStream()).use { out ->
            entries.forEach { (name, content) ->
                out.putNextEntry(ZipEntry(name))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
        return file
    }

    private companion object {
        const val BACKUP_JSON = "data.json"
        const val EMPTY_JSON = "{}"
    }
}

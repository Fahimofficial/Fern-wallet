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
        val zip = zipOf("data.json" to "{}", "nested/dir/file.txt" to "hello")

        unzip(FileInputStream(zip), destination)

        File(destination, "data.json").readText() shouldBe "{}"
        File(destination, "nested/dir/file.txt").readText() shouldBe "hello"
    }

    @Test
    fun `rejects an entry that climbs out of the destination`() {
        val victim = File(workDir, "shared_prefs/ivy_wallet_prefs.xml")
        val zip = zipOf("data.json" to "{}", "../../shared_prefs/ivy_wallet_prefs.xml" to "<map/>")

        assertThrows(ZipException::class.java) {
            unzip(FileInputStream(zip), destination)
        }

        victim.exists() shouldBe false
    }

    @Test
    fun `keeps an absolute entry name inside the destination`() {
        val victim = File(workDir, "absolute.txt")
        val zip = zipOf(victim.absolutePath to "x")

        // An absolute name is resolved under the destination by File(parent, child),
        // so it must land inside it and never at the absolute location.
        unzip(FileInputStream(zip), destination)

        victim.exists() shouldBe false
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
}

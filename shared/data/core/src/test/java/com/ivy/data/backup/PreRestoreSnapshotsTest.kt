package com.ivy.data.backup

import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreRestoreSnapshotsTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val snapshots by lazy { PreRestoreSnapshots { tempFolder.root.resolve("pre-restore") } }

    @Test
    fun `no snapshot at first`() {
        snapshots.latest() shouldBe null
    }

    @Test
    fun `latest returns the newest copy, readable as a backup`() {
        snapshots.save("""{"n":1}""", timestampMillis = 1_000)
        snapshots.save("""{"n":2}""", timestampMillis = 2_000)

        val latest = snapshots.latest()!!
        latest.inputStream().use { BackupArchive.readJson(it) } shouldBe """{"n":2}"""
    }

    @Test
    fun `keeps only the most recent copies`() {
        (1..5).forEach { snapshots.save("""{"n":$it}""", timestampMillis = it * 1_000L) }

        val files = tempFolder.root.resolve("pre-restore").listFiles()!!.map { it.name }.sorted()
        files.size shouldBe PreRestoreSnapshots.KEEP
        files shouldBe listOf("pre-restore-3000.zip", "pre-restore-4000.zip", "pre-restore-5000.zip")
    }

    @Test
    fun `delete removes the copy`() {
        snapshots.save("{}", timestampMillis = 1)
        snapshots.delete(snapshots.latest()!!)

        snapshots.latest() shouldBe null
    }
}

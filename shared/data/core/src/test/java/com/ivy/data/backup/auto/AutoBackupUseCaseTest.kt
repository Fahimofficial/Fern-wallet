package com.ivy.data.backup.auto

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeProvider
import com.ivy.data.backup.BackupArchive
import com.ivy.data.backup.BackupCrypto
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AutoBackupUseCaseTest {
    private val json = """{"accounts":[]}"""

    private class FakeFolder : BackupFolder {
        val files = linkedMapOf<String, ByteArrayOutputStream>()
        var failWrites = false

        override fun create(name: String, mimeType: String): OutputStream {
            val out = ByteArrayOutputStream()
            files[name] = out
            if (!failWrites) return out
            return object : OutputStream() {
                override fun write(b: Int): Unit = throw IOException("disk full")
            }
        }

        override fun files(): List<BackupFolderFile> = files.keys.map { BackupFolderFile(name = it, id = it) }
        override fun delete(file: BackupFolderFile): Boolean = files.remove(file.id) != null
    }

    private class FakeAccess(val folder: FakeFolder?) : BackupFolderAccess {
        override fun open(treeUri: String): BackupFolder? = folder
        override fun displayName(treeUri: String): String = "Backups"
        override fun keepAccess(treeUri: String): Unit = Unit
        override fun releaseAccess(treeUri: String): Unit = Unit
    }

    /** Stand-in for the Android Keystore: reversible, but not the identity. */
    private class FakeWrapper(var broken: Boolean = false) : BackupKeyWrapper {
        override fun wrap(raw: ByteArray): ByteArray = raw.reversedArray()
        override fun unwrap(wrapped: ByteArray): ByteArray = if (broken) error("gone") else wrapped.reversedArray()
    }

    private class Clock(var now: Instant) : TimeProvider {
        override fun getZoneId(): ZoneId = ZoneOffset.UTC
        override fun utcNow(): Instant = now
        override fun localNow(): LocalDateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        override fun localDateNow(): LocalDate = localNow().toLocalDate()
        override fun localTimeNow(): LocalTime = localNow().toLocalTime()
    }

    private fun inMemoryPrefs(): SharedPrefs {
        val values = mutableMapOf<String, Any?>()
        return mockk(relaxed = true) {
            every { putString(any(), any()) } answers { values[firstArg()] = secondArg<String?>() }
            every { getString(any(), any()) } answers { values[firstArg()] as String? ?: secondArg() }
            every { putLong(any(), any()) } answers { values[firstArg()] = secondArg<Long>() }
            every { getLong(any(), any()) } answers { values[firstArg()] as Long? ?: secondArg() }
            every { putInt(any(), any()) } answers { values[firstArg()] = secondArg<Int>() }
            every { getInt(any(), any()) } answers { values[firstArg()] as Int? ?: secondArg() }
        }
    }

    private val folder = FakeFolder()
    private val clock = Clock(Instant.parse("2026-10-30T20:15:00Z"))
    private val wrapper = FakeWrapper()
    private val preferences = AutoBackupPreferences(inMemoryPrefs())

    private fun useCase(access: BackupFolderAccess = FakeAccess(folder)) = AutoBackupUseCase(
        preferences = preferences,
        folderAccess = access,
        keyWrapper = wrapper,
        backupWriter = object : BackupWriter {
            override suspend fun writeBackup(output: OutputStream, key: BackupCrypto.Key?) =
                BackupArchive.writeBackup(output, json, key)
        },
        timeProvider = clock,
        dispatchers = TestDispatchersProvider,
    )

    @Test
    fun `without a folder nothing is written and the reason is saved`() = runTest {
        useCase().backUpNow() shouldBe AutoBackupFailure.FOLDER_UNAVAILABLE

        preferences.lastFailure shouldBe AutoBackupFailure.FOLDER_UNAVAILABLE
        preferences.lastSuccessMillis shouldBe null
    }

    @Test
    fun `writes a readable, timestamped backup`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")

        useCase.backUpNow() shouldBe null

        val (name, bytes) = folder.files.entries.single()
        name shouldBe "FernWallet-auto-2026-10-30_201500Z.zip"
        BackupArchive.readJson(bytes.toByteArray().inputStream()) shouldBe json
        preferences.lastSuccessMillis shouldBe clock.now.toEpochMilli()
        preferences.lastFailure shouldBe null
    }

    @Test
    fun `with a password the backup is encrypted and opens with that password`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")
        useCase.setPassword("s3cret".toCharArray())

        useCase.backUpNow() shouldBe null

        val (name, bytes) = folder.files.entries.single()
        name shouldEndWith ".fernbackup"
        BackupArchive.readJson(bytes.toByteArray().inputStream(), password = "s3cret".toCharArray()) shouldBe json
        useCase.isPasswordProtected() shouldBe true
    }

    @Test
    fun `a key that can't be unwrapped stops the backup`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")
        useCase.setPassword("s3cret".toCharArray())
        wrapper.broken = true

        useCase.backUpNow() shouldBe AutoBackupFailure.KEY_UNAVAILABLE
        folder.files.size shouldBe 0
    }

    @Test
    fun `keeps only the newest backups and never touches other files`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")
        folder.files["my-notes.txt"] = ByteArrayOutputStream()

        repeat(AutoBackupUseCase.KEEP + 3) { day ->
            clock.now = Instant.parse("2026-10-01T08:00:00Z").plusSeconds(day * 86_400L)
            useCase.backUpNow() shouldBe null
        }

        val backups = folder.files.keys.filter { it.startsWith(AutoBackupUseCase.PREFIX) }.sorted()
        backups shouldHaveSize AutoBackupUseCase.KEEP
        backups.first() shouldStartWith "FernWallet-auto-2026-10-04"
        folder.files.containsKey("my-notes.txt") shouldBe true
    }

    @Test
    fun `a failed write is reported and leaves no partial file`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")
        folder.failWrites = true

        useCase.backUpNow() shouldBe AutoBackupFailure.WRITE_FAILED
        folder.files.size shouldBe 0
    }

    @Test
    fun `removing the password makes later backups unencrypted`() = runTest {
        val useCase = useCase()
        useCase.setFolder("content://tree/backups")
        useCase.setPassword("s3cret".toCharArray())
        useCase.setPassword(null)

        useCase.backUpNow() shouldBe null

        useCase.isPasswordProtected() shouldBe false
        folder.files.keys.single() shouldEndWith ".zip"
    }
}

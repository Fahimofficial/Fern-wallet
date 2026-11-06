package com.ivy.base.media

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AccountMediaTest {
    private val dir = Files.createTempDirectory("media").toFile()

    @Test
    fun `only files directly inside the media folder are accepted`() {
        AccountMedia.ownedFile(dir, File(dir, "a.png").path) shouldBe File(dir, "a.png").canonicalFile
        // A crafted backup can't point the app at its database or anything outside the folder.
        AccountMedia.ownedFile(dir, File(dir, "../databases/ivywallet.db").path) shouldBe null
        AccountMedia.ownedFile(dir, "/etc/passwd") shouldBe null
        AccountMedia.ownedFile(dir, File(dir, "sub/a.png").path) shouldBe null
        AccountMedia.ownedFile(dir, "") shouldBe null
        AccountMedia.ownedFile(dir, null) shouldBe null
    }
}

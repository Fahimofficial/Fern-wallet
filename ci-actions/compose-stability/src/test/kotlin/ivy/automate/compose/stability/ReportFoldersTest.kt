package ivy.automate.compose.stability

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ReportFoldersTest {

    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("compose-reports").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `fails when no compiler reports exist`() {
        File(root, "app/build/intermediates").mkdirs()

        val folders = findComposeReportFolders(root)

        folders shouldBe emptyList()
        missingReportsError(folders).shouldNotBeNull() shouldContain ComposeReportFolderName
    }

    @Test
    fun `finds report folders in every module`() {
        File(root, "app/build/$ComposeReportFolderName").mkdirs()
        File(root, "feature/home/build/$ComposeReportFolderName").mkdirs()

        val folders = findComposeReportFolders(root)

        folders.map { it.relativeTo(root).path }.sorted() shouldBe listOf(
            "app/build/$ComposeReportFolderName",
            "feature/home/build/$ComposeReportFolderName",
        )
        missingReportsError(folders).shouldBeNull()
    }
}

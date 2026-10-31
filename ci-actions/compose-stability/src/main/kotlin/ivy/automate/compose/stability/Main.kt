package ivy.automate.compose.stability

import arrow.core.Either
import arrow.core.identity
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import ivy.automate.compose.stability.model.ComposableArgument
import ivy.automate.compose.stability.model.FullyQualifiedName
import ivy.automate.compose.stability.model.UnstableComposable
import java.io.File
import kotlin.system.exitProcess

const val OutputReportFileName = "ivy-compose-stability-report.txt"
const val ComposeReportFolderName = "compose_compiler"
const val BaselineArg = "generateBaseline"
const val BaselineFileName = "ivy-compose-stability-baseline.txt"

// The task runs with the module directory as working directory.
private const val RootDirRelativePath = "../../"

fun main(args: Array<String>) {
    val shouldGenerateBaseline = BaselineArg in args
    val baselineComposables = readBaseline()
    val reportFolders = findComposeReportFolders(File(RootDirRelativePath))
    missingReportsError(reportFolders)?.let { error ->
        println(error)
        exitProcess(1)
    }
    val unstableComposables = reportFolders
        .flatMap { reportFolder ->
            unstableComposables(reportFolder).fold(
                ifRight = ::identity,
                ifLeft = { error ->
                    println(error)
                    emptyList()
                }
            )
        }.toList()
        .filter {
            it.isNotViewModelFunction() &&
                    (shouldGenerateBaseline || it.fullyQualifiedName !in baselineComposables)
        }
    val ivyReportTxt = buildIvyReport(unstableComposables)
    createReportFile(ivyReportTxt)
    println(ivyReportTxt)
    if (!shouldGenerateBaseline) {
        if (unstableComposables.isNotEmpty()) {
            println("ERROR: ${unstableComposables.size} unstable composables found. Fix them!")
            exitProcess(1)
        } else {
            println("SUCCESS!")
        }
    } else {
        generateBaseline(unstableComposables)
        println("Baseline generated.")
        println("Check: ${File(BaselineFileName).absolutePath}")
    }
}

internal fun findComposeReportFolders(root: File): List<File> =
    root.walk()
        .filter { it.isDirectory && it.name == ComposeReportFolderName }
        .toList()

/**
 * Without compiler reports there is nothing to analyse, so passing would hide a broken setup
 * (e.g. the Compose compiler reports were turned off or the app wasn't compiled first).
 */
internal fun missingReportsError(reportFolders: List<File>): String? =
    if (reportFolders.isEmpty()) {
        "ERROR: no '$ComposeReportFolderName' report folders found. " +
            "Compile the app first (e.g. :app:assembleRelease)."
    } else {
        null
    }

private fun unstableComposables(
    reportFolder: File
): Either<String, List<UnstableComposable>> = either {
    val files = reportFolder.listFiles()
        ?: raise("Empty report folder '${reportFolder.absoluteFile}'")
    val composablesTxt = files.firstOrNull {
        it.name.endsWith("composables.txt")
    }?.readText() ?: raise("Couldn't find '*composables.txt' in '${reportFolder.absoluteFile}'")
    val composablesCsv = files.firstOrNull {
        it.name.endsWith("composables.csv")
    } ?: raise("Couldn't find '*composables.csv' in '${reportFolder.absoluteFile}'")

    val abstractComposableNames = findAbstractComposableNames(composablesTxt)
    parseUnstableComposables(composablesCsv).bind()
        .filter { it.fullyQualifiedName !in abstractComposableNames }
        .map {
            it.copy(
                unstableArguments = it.findUnstableArguments(composablesTxt).toSet()
            )
        }
}

private val abstractComposableRegex = Regex("""^\s*abstract fun ([\w.$]+)\(""", RegexOption.MULTILINE)

// Abstract composables (e.g. interface members) have no body, so they can never be
// restartable/skippable. Their implementations are reported and checked on their own.
private fun findAbstractComposableNames(composablesTxt: String): Set<FullyQualifiedName> =
    abstractComposableRegex.findAll(composablesTxt)
        .map { it.groupValues[1] }
        .toSet()

@Suppress("MagicNumber")
private fun parseUnstableComposables(
    composablesCsv: File
): Either<String, List<UnstableComposable>> =
    catch({
        composablesCsv.readText()
            .split("\n") // rows
            .drop(1) // drop the header
            .filter { it.isNotBlank() }
            .map { row ->
                val values = row.split(",")
                val fullyQualifiedName = values[0]
                val name = values[1]
                val skippable = values[3].toInt() == 1
                val restartable = values[4].toInt() == 1

                UnstableComposable(
                    fullyQualifiedName = fullyQualifiedName,
                    name = name,
                    restartable = restartable,
                    skippable = skippable,
                    unstableArguments = emptySet(),
                )
            }
            .filter { !it.restartable || !it.skippable }
            .right()
    }) {
        Either.Left("CSV parse error for '${composablesCsv.path}': ${it.message}")
    }

private fun UnstableComposable.findUnstableArguments(
    composablesTxt: String
): List<ComposableArgument> {
    val composableFunction = composablesTxt.split(")\n").firstOrNull { funTxt ->
        "fun $name(" in funTxt
    } ?: return emptyList()
    return composableFunction.split("\n")
        .drop(1) // drop the signature
        .mapNotNull { paramTxt ->
            try {
                if ("unstable" in paramTxt) {
                    val words = paramTxt.split(" ")
                        .filter { it.isNotBlank() }
                    ComposableArgument(
                        name = words[1].dropLast(1), // drop the ":"
                        type = words[2]
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}

private fun buildIvyReport(
    unstableComposables: List<UnstableComposable>
): String = buildString {
    append("-----------")
    append("\n")
    append("UNSTABLE COMPOSABLES:")
    append("\n")
    unstableComposables.forEachIndexed { index, composable ->
        append("@Composable ${composable.fullyQualifiedName}")
        append("(restartable = ${composable.restartable}, skippable = ${composable.skippable}):\n")
        composable.unstableArguments.forEach { arg ->
            append("-unstable ")
            append("\"${arg.name}: ${arg.type}\"")
            append("\n")
        }
        if (index != unstableComposables.lastIndex) {
            append("\n")
        }
    }
    append("-----------\n")
    append("[CONCLUSION]\n")
    append("Unstable Composables: ${unstableComposables.size}")
}

private fun createReportFile(report: String) {
    val reportFile = File(OutputReportFileName)
    reportFile.writeText(report)
}

private fun generateBaseline(unstableComposables: List<UnstableComposable>) {
    val baselineContent = unstableComposables.joinToString(separator = "\n") {
        it.fullyQualifiedName
    }
    val baselineFile = File(BaselineFileName)
    baselineFile.writeText(baselineContent)
}

private fun readBaseline(): Set<FullyQualifiedName> {
    return try {
        val baselineFile = File(BaselineFileName)
        baselineFile.readText().split("\n").toSet()
    } catch (e: Exception) {
        emptySet()
    }
}

private fun UnstableComposable.isNotViewModelFunction(): Boolean {
    return "ViewModel." !in fullyQualifiedName
}

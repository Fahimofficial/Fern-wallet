package ivy.automate.base

import arrow.core.raise.Raise
import arrow.core.raise.ensureNotNull

fun List<String>.parseAsMap(): Map<String, String> {
    return mapNotNull { arg ->
        val values = arg.split("=")
            .takeIf { it.size == 2 } ?: return@mapNotNull null
        values[0] to values[1]
    }.toMap()
}

fun Raise<String>.ensureArgument(args: Map<String, String>, key: String): String {
    val value = args[key]
    ensureNotNull(value) {
        "Argument '$key' is missing."
    }
    return value
}

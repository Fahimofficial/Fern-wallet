package com.ivy.navigation

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Saves the back stack in the activity's saved state, so Android killing the app in the
 * background (process death) brings the user back to the same screen instead of Home.
 *
 * Only the screens' arguments are saved, never screen content. Screens that were opened with
 * an in-memory list of transactions (e.g. from a report) can't be rebuilt from their arguments;
 * the saved stack stops before the first of those.
 */
object NavigationState {
    // Several screens have a `type` argument, so the class discriminator needs another name.
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "screen"
    }
    private val serializer = ListSerializer(Screen.serializer())

    /** Never throws: it runs while the activity saves its state. */
    fun encode(screens: List<Screen>): String = try {
        json.encodeToString(serializer, screens.takeWhile(::isRestorable))
    } catch (e: IllegalArgumentException) {
        // Includes SerializationException. Losing the stack is better than crashing.
        ""
    }

    /** The saved stack, or null if [encoded] is missing or unreadable (e.g. after an app update). */
    fun decode(encoded: String?): List<Screen>? {
        if (encoded.isNullOrBlank()) return null
        return try {
            json.decodeFromString(serializer, encoded).takeIf { it.isNotEmpty() }
        } catch (e: IllegalArgumentException) {
            // Includes SerializationException: a screen was renamed or its arguments changed.
            null
        }
    }

    fun isRestorable(screen: Screen): Boolean = when (screen) {
        is TransactionsScreen -> screen.transactions.isEmpty()
        is PieChartStatisticScreen -> screen.transactions.isEmpty() && screen.accountList.isEmpty()
        else -> true
    }
}

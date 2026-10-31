package com.ivy.legacy.planned

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun day(text: String): Instant = LocalDate.parse(text).atTime(9, 0).toInstant(ZoneOffset.UTC)

internal fun List<Instant>.days(): List<String> = map { it.atZone(ZoneOffset.UTC).toLocalDate().toString() }

/** "Today" in the planned-payment tests. */
internal val NOW: Instant = day("2026-10-30")

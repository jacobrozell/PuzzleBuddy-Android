package com.jacobrozell.puzzlebuddy.domain.catalog

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object MediumDateLabel {
    fun format(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .format(instant.atZone(zoneId))
}

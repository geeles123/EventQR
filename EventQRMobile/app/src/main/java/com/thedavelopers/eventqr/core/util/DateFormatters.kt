package com.thedavelopers.eventqr.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormatters {
    private val displayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMM d, yyyy • h:mm a")
        .withZone(ZoneId.systemDefault())

    fun formatInstant(value: Instant?): String {
        return if (value == null) "-" else displayFormatter.format(value)
    }
}

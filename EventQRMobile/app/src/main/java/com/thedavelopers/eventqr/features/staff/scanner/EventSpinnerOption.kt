package com.thedavelopers.eventqr.features.staff

import java.time.Instant

data class EventSpinnerOption(
    val id: String,
    val label: String,
    val canScan: Boolean,
    val eventStartAt: Instant? = null,
)
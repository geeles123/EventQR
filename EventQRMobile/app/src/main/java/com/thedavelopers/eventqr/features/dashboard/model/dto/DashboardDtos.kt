package com.thedavelopers.eventqr.features.dashboard.model.dto

import java.time.Instant
import java.util.UUID

data class DashboardSummary(
    val totalEvents: Long,
    val totalRegistrations: Long,
    val totalTransactions: Long,
    val totalRewards: Long,
    val totalNotifications: Long,
    val fullName: String? = null,
    val upcomingEvents: List<DashboardUpcomingEvent>? = emptyList(),
)

data class DashboardUpcomingEvent(
    val eventId: UUID,
    val registrationId: UUID,
    val title: String,
    val eventStartAt: Instant? = null,
    val status: String? = null,
)

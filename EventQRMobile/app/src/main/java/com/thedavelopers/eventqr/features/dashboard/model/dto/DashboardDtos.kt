package com.thedavelopers.eventqr.features.dashboard.model.dto

data class DashboardSummary(
    val totalEvents: Long,
    val totalRegistrations: Long,
    val totalTransactions: Long,
    val totalRewards: Long,
    val totalNotifications: Long,
)

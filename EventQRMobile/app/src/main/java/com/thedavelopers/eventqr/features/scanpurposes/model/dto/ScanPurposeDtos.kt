package com.thedavelopers.eventqr.features.scanpurposes.model.dto

import com.thedavelopers.eventqr.core.api.dto.ScanPurposeCode
import java.util.UUID

data class ScanPurposeRequest(
    val eventId: UUID,
    val name: String,
    val code: ScanPurposeCode,
    val active: Boolean = true,
    val trackingOnly: Boolean = false,
    val description: String? = null,
)

data class ScanPurposeResponse(
    val scanPurposeId: UUID,
    val eventId: UUID,
    val name: String,
    val code: ScanPurposeCode,
    val active: Boolean,
    val trackingOnly: Boolean,
    val description: String? = null,
)

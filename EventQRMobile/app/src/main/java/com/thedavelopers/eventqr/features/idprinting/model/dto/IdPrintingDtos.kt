package com.thedavelopers.eventqr.features.idprinting.model.dto

import java.time.Instant
import java.util.UUID

data class IdPrintRequest(
    val eventId: UUID,
    val qrCredentialId: UUID,
    val staffUserId: UUID,
    val reprint: Boolean = false,
)

data class IdPrintResponse(
    val printLogId: UUID,
    val eventId: UUID,
    val attendeeUserId: UUID,
    val registrationId: UUID,
    val qrCredentialId: UUID,
    val templateId: UUID,
    val reprint: Boolean,
    val success: Boolean,
    val message: String,
    val printedAt: Instant? = null,
)

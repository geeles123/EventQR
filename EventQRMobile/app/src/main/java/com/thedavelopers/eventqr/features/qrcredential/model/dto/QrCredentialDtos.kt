package com.thedavelopers.eventqr.features.qrcredential.model.dto

import com.thedavelopers.eventqr.core.api.dto.QrDeliveryStatus
import com.thedavelopers.eventqr.core.api.dto.QrDisplayStatus
import java.util.UUID

data class QrCredentialSnapshot(
    val qrCredentialId: UUID,
    val eventId: UUID,
    val attendeeUserId: UUID,
    val registrationId: UUID,
    val qrValue: String,
    val active: Boolean,
    val displayStatus: QrDisplayStatus,
    val deliveryStatus: QrDeliveryStatus,
    val downloaded: Boolean,
)

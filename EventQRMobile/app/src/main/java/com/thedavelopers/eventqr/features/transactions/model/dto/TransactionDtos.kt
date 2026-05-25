package com.thedavelopers.eventqr.features.transactions.model.dto

import com.thedavelopers.eventqr.core.api.dto.TransactionResult
import com.thedavelopers.eventqr.core.api.dto.TransactionType
import java.time.Instant
import java.util.UUID

data class TransactionRequest(
    val eventId: UUID,
    val scanPurposeId: UUID,
    val qrValue: String,
    val staffUserId: UUID? = null,
    val notes: String? = null,
)

data class TransactionResponse(
    val transactionId: UUID,
    val eventId: UUID,
    val attendeeUserId: UUID,
    val registrationId: UUID,
    val qrCredentialId: UUID,
    val scanPurposeId: UUID,
    val transactionType: TransactionType,
    val transactionResult: TransactionResult,
    val pointsDelta: Int,
    val reason: String? = null,
    val scannedAt: Instant? = null,
)

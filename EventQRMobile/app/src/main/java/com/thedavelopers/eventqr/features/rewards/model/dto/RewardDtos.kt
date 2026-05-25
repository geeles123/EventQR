package com.thedavelopers.eventqr.features.rewards.model.dto

import com.thedavelopers.eventqr.core.api.dto.RedemptionStatus
import com.thedavelopers.eventqr.core.api.dto.RewardStatus
import java.time.Instant
import java.util.UUID

data class PointBalanceResponse(
    val eventId: UUID,
    val attendeeUserId: UUID,
    val pointsBalance: Int,
)

data class PointRuleRequest(
    val eventId: UUID,
    val scanPurposeId: UUID,
    val points: Int,
    val active: Boolean,
)

data class PointRuleResponse(
    val id: UUID,
    val eventId: UUID,
    val scanPurposeId: UUID,
    val points: Int,
    val active: Boolean,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

data class RewardRedemptionRequest(
    val eventId: UUID,
    val attendeeUserId: UUID,
    val rewardId: UUID,
)

data class RewardRedemptionResponse(
    val redemptionId: UUID,
    val eventId: UUID,
    val attendeeUserId: UUID,
    val rewardId: UUID,
    val pointsSpent: Int,
    val status: RedemptionStatus,
    val redeemedAt: Instant? = null,
    val reason: String? = null,
)

data class RewardRequest(
    val eventId: UUID,
    val name: String,
    val pointsRequired: Int,
    val stockQuantity: Int? = null,
)

data class RewardResponse(
    val rewardId: UUID,
    val eventId: UUID,
    val name: String,
    val pointsRequired: Int,
    val status: RewardStatus,
    val stockQuantity: Int? = null,
)

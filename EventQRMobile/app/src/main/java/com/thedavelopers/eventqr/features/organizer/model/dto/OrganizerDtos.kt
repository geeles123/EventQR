package com.thedavelopers.eventqr.features.organizer.model.dto

import com.thedavelopers.eventqr.core.api.dto.ScanPurposeCode
import com.thedavelopers.eventqr.core.api.dto.TransactionResult
import com.thedavelopers.eventqr.core.api.dto.TransactionType
import java.time.Instant
import java.util.UUID

data class OrganizerEventDto(
    val eventId: UUID,
    val title: String,
    val organizerName: String? = null,
    val dateTime: String? = null,
    val shortDate: String? = null,
    val venue: String? = null,
    val status: String? = null,
    val submittedDate: String? = null,
    val adminRemarks: String? = null,
    val description: String? = null,
    val eventStartAt: Instant? = null,
    val eventEndAt: Instant? = null,
    val registrationOpenAt: Instant? = null,
    val registrationCloseAt: Instant? = null,
    val capacity: Int = 0,
    val currentAttendeeCount: Int = 0,
    val availableSlots: Int = 0,
    val additionalOrganizers: List<String> = emptyList(),
    val registeredCount: Int = 0,
    val enteredCount: Int = 0,
    val attendedCount: Int = 0,
    val exitedCount: Int = 0,
    val noShowCount: Int = 0,
    val totalTransactions: Int = 0,
    val successfulScans: Int = 0,
    val rejectedScans: Int = 0,
    val benefitClaims: Int = 0,
    val boothSessionVisits: Int = 0,
    val rewardRedemptions: Int = 0,
    val totalPointsAwarded: Int = 0,
    val idTemplateStatus: String? = null,
    val rewardsStatus: String? = null,
    val staffCount: Int = 0,
    val scanPurposesCount: Int = 0,
)

data class OrganizerDashboardDto(
    val organizerUserId: UUID? = null,
    val organizerName: String? = null,
    val organizerEmail: String? = null,
    val organization: String? = null,
    val totalEvents: Int = 0,
    val totalAttendees: Int = 0,
    val totalTransactions: Int = 0,
    val totalPointsAwarded: Int = 0,
    val rewardsSummary: String? = null,
    val recentEvents: List<OrganizerEventDto> = emptyList(),
    val event: OrganizerEventDto? = null,
)

data class OrganizerAttendeeDto(
    val attendeeId: UUID? = null,
    val registrationId: UUID,
    val eventId: UUID,
    val qrCredentialId: UUID? = null,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val registrationStatus: String? = null,
    val currentEventStatus: String? = null,
    val points: Int = 0,
    val lastTransactionTime: String? = null,
    val registeredDate: String? = null,
    val qrCredentialStatus: String? = null,
    val recentTransactions: List<String> = emptyList(),
    val recentRejectedScans: List<String> = emptyList(),
)

data class OrganizerTransactionDto(
    val transactionId: UUID,
    val eventId: UUID,
    val eventTitle: String? = null,
    val attendeeId: UUID? = null,
    val attendeeName: String? = null,
    val attendeeEmail: String? = null,
    val registrationId: UUID? = null,
    val qrCredentialId: UUID? = null,
    val scanPurposeId: UUID? = null,
    val staffId: UUID? = null,
    val staffName: String? = null,
    val staffEmail: String? = null,
    val qrId: String? = null,
    val scanPurpose: String? = null,
    val transactionType: TransactionType,
    val resultStatus: TransactionResult,
    val pointsDelta: Int = 0,
    val reason: String? = null,
    val message: String? = null,
    val deviceSource: String? = null,
    val relatedItem: String? = null,
    val createdTimestamp: Instant? = null,
)

data class OrganizerReportDto(
    val eventId: UUID,
    val totalRegistered: Int = 0,
    val enteredCount: Int = 0,
    val exitedCount: Int = 0,
    val attendanceCount: Int = 0,
    val noShowCount: Int = 0,
    val approvedTransactionCount: Int = 0,
    val rejectedTransactionCount: Int = 0,
    val pointsDistributed: Int = 0,
    val benefitClaims: Int = 0,
    val boothSessionVisits: Int = 0,
    val rewardRedemptions: Int = 0,
    val rejectedScans: Int = 0,
    val transactionSummary: List<OrganizerReportRowDto> = emptyList(),
    val attendanceSummary: List<OrganizerReportRowDto> = emptyList(),
    val rejectedSummary: List<OrganizerReportRowDto> = emptyList(),
    val pointsRewardsSummary: List<OrganizerReportRowDto> = emptyList(),
    val recentActivity: List<OrganizerReportRowDto> = emptyList(),
)

data class OrganizerOverallReportDto(
    val organizerUserId: UUID,
    val organizerName: String? = null,
    val totalEvents: Int = 0,
    val totalRegistered: Int = 0,
    val enteredCount: Int = 0,
    val exitedCount: Int = 0,
    val attendanceCount: Int = 0,
    val approvedTransactionCount: Int = 0,
    val rejectedTransactionCount: Int = 0,
    val pointsDistributed: Int = 0,
    val benefitClaims: Int = 0,
    val boothSessionVisits: Int = 0,
    val rewardRedemptions: Int = 0,
    val eventBreakdown: List<OrganizerOverallEventReportDto> = emptyList(),
)

data class OrganizerOverallEventReportDto(
    val eventId: UUID,
    val eventTitle: String,
    val registered: Int = 0,
    val entered: Int = 0,
    val exited: Int = 0,
    val approvedScans: Int = 0,
    val rejectedScans: Int = 0,
    val points: Int = 0,
)

data class OrganizerReportRowDto(
    val label: String,
    val value: String,
)

data class OrganizerStaffDto(
    val assignmentId: UUID,
    val eventId: UUID,
    val staffUserId: UUID,
    val name: String? = null,
    val email: String? = null,
    val roleLabel: String? = null,
    val active: Boolean = true,
    val canScan: Boolean = false,
    val canPrintId: Boolean = false,
    val canViewLogs: Boolean = false,
    val canManageRewards: Boolean = false,
    val permissions: List<String> = emptyList(),
    val addedAt: Instant? = null,
)

data class StaffAssignmentRequestDto(
    val staffUserId: UUID? = null,
    val email: String? = null,
    val name: String? = null,
    val roleLabel: String? = null,
    val canScan: Boolean? = null,
    val canPrintId: Boolean? = null,
    val canViewLogs: Boolean? = null,
    val canManageRewards: Boolean? = null,
    val permissions: List<String> = emptyList(),
)

data class StaffAssignmentUpdateRequestDto(
    val active: Boolean? = null,
    val roleLabel: String? = null,
    val canScan: Boolean? = null,
    val canPrintId: Boolean? = null,
    val canViewLogs: Boolean? = null,
    val canManageRewards: Boolean? = null,
    val permissions: List<String>? = null,
)

data class OrganizerScanPurposeDto(
    val scanPurposeId: UUID? = null,
    val eventId: UUID,
    val title: String,
    val description: String? = null,
    val code: ScanPurposeCode,
    val enabled: Boolean = false,
    val trackingOnly: Boolean = true,
    val pointsEnabled: Boolean = false,
    val pointsValue: Int = 0,
    val allowDuplicate: Boolean = false,
    val duplicateRuleSummary: String? = null,
    val requiredSelectionLabel: String? = null,
)

data class OrganizerScanPurposeRequestDto(
    val scanPurposeId: UUID? = null,
    val title: String,
    val code: ScanPurposeCode,
    val enabled: Boolean,
    val trackingOnly: Boolean,
    val pointsEnabled: Boolean,
    val pointsValue: Int,
    val allowDuplicate: Boolean,
    val duplicateRuleSummary: String? = null,
    val requiredSelectionLabel: String? = null,
    val description: String? = null,
)

data class OrganizerUserSearchDto(
    val userId: UUID,
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val status: String? = null,
)

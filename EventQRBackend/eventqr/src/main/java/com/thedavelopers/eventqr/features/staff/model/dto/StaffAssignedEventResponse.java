package com.thedavelopers.eventqr.features.staff.model.dto;

import java.time.Instant;
import java.util.UUID;

import com.thedavelopers.eventqr.shared.constants.EventStatus;

public record StaffAssignedEventResponse(UUID assignmentId, UUID eventId, String title, String description,
                                         String location, Instant eventStartAt, Instant eventEndAt,
                                         EventStatus status, boolean canScan, boolean canPrintId,
                                         boolean canViewLogs, boolean canManageRewards) {
}

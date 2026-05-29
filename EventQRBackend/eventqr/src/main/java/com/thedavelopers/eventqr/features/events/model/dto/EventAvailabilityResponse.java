package com.thedavelopers.eventqr.features.events.model.dto;

import java.time.Instant;
import java.util.UUID;

public record EventAvailabilityResponse(UUID eventId, int capacity, int currentAttendeeCount, boolean registrationOpen,
                                        boolean full, boolean available, String message, Instant serverNow,
                                        Instant registrationOpenAt, Instant registrationCloseAt) {
}

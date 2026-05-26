package com.thedavelopers.eventqr.features.events.model.dto;

import java.time.Instant;
import java.util.UUID;

public record AttendeeEventResponse(UUID eventId, String title, String description, String location,
                                    Instant registrationOpenAt, Instant registrationCloseAt,
                                    Instant eventStartAt, Instant eventEndAt, int capacity,
                                    int currentAttendeeCount) {
}

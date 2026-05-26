package com.thedavelopers.eventqr.features.dashboard.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardSummary;
import com.thedavelopers.eventqr.features.dashboard.model.dto.DashboardSummary.DashboardUpcomingEvent;
import com.thedavelopers.eventqr.features.events.repository.EventRepository;
import com.thedavelopers.eventqr.features.notifications.repository.NotificationRepository;
import com.thedavelopers.eventqr.features.registrations.model.entity.EventRegistration;
import com.thedavelopers.eventqr.features.registrations.repository.EventRegistrationRepository;
import com.thedavelopers.eventqr.features.rewards.repository.AttendeePointBalanceRepository;
import com.thedavelopers.eventqr.features.transactions.repository.TransactionLogRepository;
import com.thedavelopers.eventqr.features.users.model.entity.UserProfile;
import com.thedavelopers.eventqr.features.users.repository.UserProfileRepository;
import com.thedavelopers.eventqr.shared.constants.RegistrationStatus;
import com.thedavelopers.eventqr.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final AttendeePointBalanceRepository attendeePointBalanceRepository;
    private final NotificationRepository notificationRepository;
    private final UserProfileRepository userProfileRepository;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(EventRepository eventRepository,
                            EventRegistrationRepository eventRegistrationRepository,
                            TransactionLogRepository transactionLogRepository,
                            AttendeePointBalanceRepository attendeePointBalanceRepository,
                            NotificationRepository notificationRepository,
                            UserProfileRepository userProfileRepository,
                            JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.attendeePointBalanceRepository = attendeePointBalanceRepository;
        this.notificationRepository = notificationRepository;
        this.userProfileRepository = userProfileRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummary summary(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Instant now = Instant.now();
        List<EventRegistration> registrations = eventRegistrationRepository.findByAttendeeUserId(userId);
        Set<UUID> eventIds = registrations.stream()
                .map(EventRegistration::getEventId)
                .collect(Collectors.toSet());
        Map<UUID, DashboardEventPreview> eventsById = loadEventPreviews().entrySet().stream()
                .filter(entry -> eventIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        long registeredCount = registrations.stream()
                .filter(this::isRegistered)
                .count();
        long availableEventsCount = eventRepository.count();
        long pointsCount = attendeePointBalanceRepository.sumPointsByAttendeeUserId(userId);
        List<DashboardUpcomingEvent> upcomingEvents = registrations.stream()
                .filter(this::isRegistered)
                .map(registration -> toUpcomingEvent(registration, eventsById.get(registration.getEventId()), now))
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(
                        DashboardUpcomingEvent::eventStartAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(3)
                .toList();

        return new DashboardSummary(availableEventsCount, registeredCount, transactionLogRepository.count(),
                pointsCount, notificationRepository.count(), profile.getFullName(), upcomingEvents);
    }

    private java.util.Optional<DashboardUpcomingEvent> toUpcomingEvent(EventRegistration registration, DashboardEventPreview event,
                                                                       Instant now) {
        if (event == null) {
            return java.util.Optional.empty();
        }
        Instant startAt = event.eventStartAt();
        if (startAt != null && startAt.isBefore(now)) {
            return java.util.Optional.empty();
        }
        String title = event.title() == null || event.title().isBlank() ? "Untitled event" : event.title();
        return java.util.Optional.of(new DashboardUpcomingEvent(event.eventId(), registration.getId(), title, startAt,
                formatStatus(registration.getStatus())));
    }

    private boolean isRegistered(EventRegistration registration) {
        RegistrationStatus status = registration.getStatus();
        return status != RegistrationStatus.CANCELLED && status != RegistrationStatus.NO_SHOW;
    }

    private String formatStatus(RegistrationStatus status) {
        if (status == null) {
            return "Registered";
        }
        String lower = status.name().replace('_', ' ').toLowerCase(Locale.US);
        return lower.substring(0, 1).toUpperCase(Locale.US) + lower.substring(1);
    }

    private Map<UUID, DashboardEventPreview> loadEventPreviews() {
        Map<UUID, DashboardEventPreview> eventsById = new HashMap<>();
        jdbcTemplate.query("""
                select id, title, event_start_at
                from events
                """, rs -> {
            UUID eventId = rs.getObject("id", UUID.class);
            java.sql.Timestamp eventStartAt = rs.getTimestamp("event_start_at");
            eventsById.put(eventId, new DashboardEventPreview(eventId, rs.getString("title"),
                    eventStartAt == null ? null : eventStartAt.toInstant()));
        });
        return eventsById;
    }

    private record DashboardEventPreview(UUID eventId, String title, Instant eventStartAt) {
    }
}

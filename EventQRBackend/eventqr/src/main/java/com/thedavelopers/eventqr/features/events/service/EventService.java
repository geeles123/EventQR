package com.thedavelopers.eventqr.features.events.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedavelopers.eventqr.features.events.model.dto.AttendeeEventResponse;
import com.thedavelopers.eventqr.features.events.model.dto.EventAvailabilityResponse;
import com.thedavelopers.eventqr.features.events.model.dto.EventApprovalRequest;
import com.thedavelopers.eventqr.features.events.model.dto.EventRequest;
import com.thedavelopers.eventqr.features.events.model.dto.EventResponse;
import com.thedavelopers.eventqr.features.events.model.entity.Event;
import com.thedavelopers.eventqr.features.events.repository.EventRepository;
import com.thedavelopers.eventqr.shared.constants.EventStatus;
import com.thedavelopers.eventqr.shared.exception.ConflictException;
import com.thedavelopers.eventqr.shared.exception.ResourceNotFoundException;
import com.thedavelopers.eventqr.shared.port.EventLookupPort;
import com.thedavelopers.eventqr.shared.port.EventLookupPort.EventSnapshot;

@Service
@Transactional
public class EventService implements EventLookupPort {

    private final EventRepository eventRepository;
    private final JdbcTemplate jdbcTemplate;

    public EventService(EventRepository eventRepository, JdbcTemplate jdbcTemplate) {
        this.eventRepository = eventRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public EventResponse create(EventRequest request) {
        Event event = new Event();
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setRegistrationOpenAt(request.registrationOpenAt());
        event.setRegistrationCloseAt(request.registrationCloseAt());
        event.setEventStartAt(request.eventStartAt());
        event.setEventEndAt(request.eventEndAt());
        event.setCapacity(request.capacity());
        event.setCurrentAttendeeCount(0);
        event.setRewardsEnabled(Boolean.TRUE.equals(request.rewardsEnabled()));
        event.setOrganizerUserId(request.organizerUserId());
        event.setStatus(EventStatus.PENDING_REVIEW);
        return toResponse(eventRepository.save(event));
    }

    public EventResponse review(UUID eventId, EventApprovalRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setStatus(request.approved() ? EventStatus.APPROVED : EventStatus.REJECTED);
        event.setApprovedByUserId(request.reviewerUserId());
        event.setApprovedAt(request.approved() ? java.time.Instant.now() : null);
        event.setRejectionReason(request.approved() ? null : request.rejectionReason());
        return toResponse(eventRepository.save(event));
    }

    public EventResponse activate(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (event.getStatus() != EventStatus.APPROVED) {
            throw new ConflictException("Only approved events can be activated");
        }
        event.setStatus(EventStatus.ACTIVE);
        return toResponse(eventRepository.save(event));
    }

    public List<EventResponse> findAllEvents() {
        return eventRepository.findAll().stream().map(this::toResponse).toList();
    }

        public EventResponse findOne(UUID eventId) {
        return toResponse(eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId)));
        }

        public EventAvailabilityResponse availability(UUID eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        boolean registrationOpen = event.getStatus() == EventStatus.APPROVED || event.getStatus() == EventStatus.ACTIVE;
        boolean full = (event.getCapacity() != null && event.getCapacity() > 0)
            && (event.getCurrentAttendeeCount() != null && event.getCurrentAttendeeCount() >= event.getCapacity());
        boolean available = registrationOpen && !full;
        return new EventAvailabilityResponse(event.getId(), event.getCapacity() == null ? 0 : event.getCapacity(),
            event.getCurrentAttendeeCount() == null ? 0 : event.getCurrentAttendeeCount(), registrationOpen, full,
            available, available ? "Event can accept registrations" : "Event is closed or at capacity");
        }

        public EventResponse update(UUID eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setRegistrationOpenAt(request.registrationOpenAt());
        event.setRegistrationCloseAt(request.registrationCloseAt());
        event.setEventStartAt(request.eventStartAt());
        event.setEventEndAt(request.eventEndAt());
        event.setCapacity(request.capacity());
        event.setRewardsEnabled(Boolean.TRUE.equals(request.rewardsEnabled()));
        event.setOrganizerUserId(request.organizerUserId());
        return toResponse(eventRepository.save(event));
        }

        public EventResponse updateStatus(UUID eventId, EventStatus status) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setStatus(status);
        return toResponse(eventRepository.save(event));
        }

    public List<AttendeeEventResponse> findAttendeeVisibleEvents() {
        return jdbcTemplate.query("""
                select id, title, description, location, registration_open_at, registration_close_at,
                       event_start_at, event_end_at, capacity, current_attendee_count
                from events
                order by event_start_at nulls last, created_at desc
                """, (rs, rowNum) -> new AttendeeEventResponse(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("location"),
                rs.getTimestamp("registration_open_at") == null ? null : rs.getTimestamp("registration_open_at").toInstant(),
                rs.getTimestamp("registration_close_at") == null ? null : rs.getTimestamp("registration_close_at").toInstant(),
                rs.getTimestamp("event_start_at") == null ? null : rs.getTimestamp("event_start_at").toInstant(),
                rs.getTimestamp("event_end_at") == null ? null : rs.getTimestamp("event_end_at").toInstant(),
                rs.getInt("capacity"),
                rs.getInt("current_attendee_count")));
    }

    @Override
    public EventSnapshot requireEvent(UUID eventId) {
        return eventRepository.findById(eventId).map(Event::toSnapshot)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    @Override
    public java.util.Optional<EventSnapshot> findById(UUID eventId) {
        return eventRepository.findById(eventId).map(Event::toSnapshot);
    }

    @Override
    public List<EventSnapshot> listAll() {
        return eventRepository.findAll().stream().map(Event::toSnapshot).toList();
    }

    public void incrementCurrentAttendeeCount(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        event.setCurrentAttendeeCount((event.getCurrentAttendeeCount() == null ? 0 : event.getCurrentAttendeeCount()) + 1);
        eventRepository.save(event);
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getLocation(),
                event.getRegistrationOpenAt(), event.getRegistrationCloseAt(), event.getEventStartAt(),
                event.getEventEndAt(), event.getCapacity() == null ? 0 : event.getCapacity(),
                event.getCurrentAttendeeCount() == null ? 0 : event.getCurrentAttendeeCount(), event.getStatus(),
                event.isRewardsEnabled(), event.getOrganizerUserId(), event.getApprovedByUserId(), event.getApprovedAt(),
                event.getRejectionReason());
    }
}

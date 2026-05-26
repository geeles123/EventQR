package com.thedavelopers.eventqr.features.rewards.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thedavelopers.eventqr.features.rewards.model.entity.AttendeePointBalance;

public interface AttendeePointBalanceRepository extends JpaRepository<AttendeePointBalance, UUID> {

    Optional<AttendeePointBalance> findByEventIdAndAttendeeUserId(UUID eventId, UUID attendeeUserId);

    @Query("select coalesce(sum(b.pointsBalance), 0) from AttendeePointBalance b where b.attendeeUserId = :attendeeUserId")
    long sumPointsByAttendeeUserId(@Param("attendeeUserId") UUID attendeeUserId);
}

package com.thedavelopers.eventqr.features.transactions.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thedavelopers.eventqr.features.transactions.model.entity.TransactionRule;

public interface TransactionRuleRepository extends JpaRepository<TransactionRule, UUID> {

    List<TransactionRule> findByEventId(UUID eventId);

    Optional<TransactionRule> findByEventIdAndScanPurposeId(UUID eventId, UUID scanPurposeId);
}
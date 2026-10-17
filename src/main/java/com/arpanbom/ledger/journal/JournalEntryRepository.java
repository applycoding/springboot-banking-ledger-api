package com.arpanbom.ledger.journal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<JournalEntry> findDetailedById(UUID id);
}

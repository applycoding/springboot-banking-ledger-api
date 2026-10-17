package com.arpanbom.ledger.journal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountEntryResponse(
        UUID journalEntryId,
        JournalType type,
        EntryDirection direction,
        BigDecimal amount,
        String currency,
        String description,
        Instant createdAt
) {
}

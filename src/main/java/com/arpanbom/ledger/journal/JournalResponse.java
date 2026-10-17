package com.arpanbom.ledger.journal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalResponse(
        UUID id,
        JournalType type,
        String description,
        String idempotencyKey,
        Instant createdAt,
        List<JournalLineResponse> lines
) {
}

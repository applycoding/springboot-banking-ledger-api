package com.arpanbom.ledger.journal;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalLineResponse(
        UUID accountId,
        String accountNumber,
        EntryDirection direction,
        BigDecimal amount,
        String currency
) {
}

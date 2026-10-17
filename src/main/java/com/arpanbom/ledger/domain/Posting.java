package com.arpanbom.ledger.domain;

import com.arpanbom.ledger.journal.EntryDirection;
import java.math.BigDecimal;
import java.util.UUID;

public record Posting(UUID accountId, EntryDirection direction, BigDecimal amount, String currency) {
}

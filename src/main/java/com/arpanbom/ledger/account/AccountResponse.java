package com.arpanbom.ledger.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        AccountType type,
        AccountStatus status,
        String currency,
        BigDecimal balance,
        long version,
        Instant createdAt
) {
}

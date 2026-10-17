package com.arpanbom.ledger.domain;

import com.arpanbom.ledger.account.AccountStatus;
import com.arpanbom.ledger.account.AccountType;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountSnapshot(
        UUID id,
        String accountNumber,
        AccountType type,
        AccountStatus status,
        String currency,
        BigDecimal balance
) {
}

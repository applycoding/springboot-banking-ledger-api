package com.arpanbom.ledger.account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record CreateAccountRequest(
        @NotNull UUID customerId,
        @NotNull @Pattern(regexp = "USD", message = "Only USD accounts are supported") String currency
) {
}

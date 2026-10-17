package com.arpanbom.ledger.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(Jwt jwt) {

    public record Jwt(String secret, Duration expiration) {
    }
}

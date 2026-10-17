package com.arpanbom.ledger.domain;

import java.util.regex.Pattern;

public final class IdempotencyKeys {

    private static final Pattern PATTERN = Pattern.compile("[A-Za-z0-9_.:\\-]{8,128}");

    private IdempotencyKeys() {
    }

    public static String require(String key) {
        if (key == null || !PATTERN.matcher(key).matches()) {
            throw new LedgerException.InvalidIdempotencyKey(
                    "Idempotency-Key must be 8 to 128 characters and use letters, digits, or . _ : -");
        }
        return key;
    }
}

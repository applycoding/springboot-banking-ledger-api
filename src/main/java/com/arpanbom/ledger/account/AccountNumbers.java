package com.arpanbom.ledger.account;

import java.util.concurrent.ThreadLocalRandom;

final class AccountNumbers {

    private AccountNumbers() {
    }

    static String next() {
        long value = ThreadLocalRandom.current().nextLong(0, 10_000_000_000L);
        return "LDG" + String.format("%010d", value);
    }
}

package com.arpanbom.ledger.journal;

import java.time.ZoneOffset;

final class JournalMapper {

    private JournalMapper() {
    }

    static JournalResponse toResponse(JournalEntry entry) {
        return new JournalResponse(
                entry.getId(),
                entry.getEntryType(),
                entry.getDescription(),
                entry.getIdempotencyKey(),
                entry.getCreatedAt().toInstant(ZoneOffset.UTC),
                entry.getLines().stream().map(JournalMapper::toLine).toList());
    }

    static JournalLineResponse toLine(JournalLine line) {
        return new JournalLineResponse(
                line.getAccount().getId(),
                line.getAccount().getAccountNumber(),
                line.getDirection(),
                line.getAmount(),
                line.getCurrency());
    }

    static AccountEntryResponse toAccountEntry(JournalLine line) {
        JournalEntry entry = line.getJournalEntry();
        return new AccountEntryResponse(
                entry.getId(),
                entry.getEntryType(),
                line.getDirection(),
                line.getAmount(),
                line.getCurrency(),
                entry.getDescription(),
                entry.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}

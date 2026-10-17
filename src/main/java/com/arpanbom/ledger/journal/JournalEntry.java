package com.arpanbom.ledger.journal;

import com.arpanbom.ledger.account.Account;
import com.arpanbom.ledger.domain.Posting;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "journal_entry")
public class JournalEntry {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private JournalType entryType;

    @Column(nullable = false, length = 280)
    private String description;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    protected JournalEntry() {
    }

    public static JournalEntry record(
            JournalType entryType,
            String description,
            String idempotencyKey,
            String requestHash,
            List<Posting> postings,
            Map<UUID, Account> accounts) {
        JournalEntry entry = new JournalEntry();
        entry.id = UUID.randomUUID();
        entry.entryType = entryType;
        entry.description = description;
        entry.idempotencyKey = idempotencyKey;
        entry.requestHash = requestHash;
        entry.createdAt = LocalDateTime.now();
        for (Posting posting : postings) {
            Account account = accounts.get(posting.accountId());
            entry.lines.add(JournalLine.of(entry, account, posting.direction(), posting.amount(), posting.currency()));
        }
        return entry;
    }

    public UUID getId() {
        return id;
    }

    public JournalType getEntryType() {
        return entryType;
    }

    public String getDescription() {
        return description;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<JournalLine> getLines() {
        return List.copyOf(lines);
    }
}

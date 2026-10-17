package com.arpanbom.ledger.journal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    @Query("""
            select line from JournalLine line
            join fetch line.journalEntry entry
            where line.account.id = :accountId
            order by entry.createdAt asc, line.id asc
            """)
    List<JournalLine> findForAccount(UUID accountId);
}

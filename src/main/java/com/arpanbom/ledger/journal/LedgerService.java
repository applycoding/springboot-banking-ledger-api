package com.arpanbom.ledger.journal;

import com.arpanbom.ledger.account.Account;
import com.arpanbom.ledger.account.AccountRepository;
import com.arpanbom.ledger.account.AccountType;
import com.arpanbom.ledger.domain.IdempotencyKeys;
import com.arpanbom.ledger.domain.LedgerException;
import com.arpanbom.ledger.domain.Posting;
import com.arpanbom.ledger.domain.RequestHashes;
import com.arpanbom.ledger.domain.TransferRules;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.OptimisticLockException;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private static final int MAX_ATTEMPTS = 8;

    private final TransactionTemplate tx;
    private final AccountRepository accounts;
    private final JournalEntryRepository journals;
    private final JournalLineRepository lines;

    public LedgerService(
            PlatformTransactionManager transactions,
            AccountRepository accounts,
            JournalEntryRepository journals,
            JournalLineRepository lines) {
        this.tx = new TransactionTemplate(transactions);
        this.tx.setTimeout(5);
        this.accounts = accounts;
        this.journals = journals;
        this.lines = lines;
    }

    public JournalResult deposit(UUID accountId, BigDecimal amount, String description, String idempotencyKey) {
        BigDecimal normalized = TransferRules.normalizeAmount(amount);
        String text = text(description, "Deposit");
        String key = IdempotencyKeys.require(idempotencyKey);
        String hash = RequestHashes.of("DEPOSIT", accountId, normalized.toPlainString(), text);
        return withIdempotency(key, hash, () -> postDeposit(accountId, normalized, text, key, hash));
    }

    public JournalResult transfer(
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            String description,
            String idempotencyKey) {
        BigDecimal normalized = TransferRules.normalizeAmount(amount);
        String text = text(description, "Transfer");
        String key = IdempotencyKeys.require(idempotencyKey);
        String hash = RequestHashes.of(
                "TRANSFER", fromAccountId, toAccountId, normalized.toPlainString(), text);
        return withIdempotency(key, hash, () -> postTransfer(fromAccountId, toAccountId, normalized, text, key, hash));
    }

    @Transactional(readOnly = true)
    public JournalResponse get(UUID id) {
        JournalEntry entry = journals.findDetailedById(id)
                .orElseThrow(() -> new LedgerException.NotFound("Transfer " + id + " was not found"));
        return JournalMapper.toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<AccountEntryResponse> entries(UUID accountId) {
        if (!accounts.existsById(accountId)) {
            throw new LedgerException.NotFound("Account " + accountId + " was not found");
        }
        return lines.findForAccount(accountId).stream().map(JournalMapper::toAccountEntry).toList();
    }

    private JournalResult withIdempotency(String key, String hash, Supplier<JournalResult> action) {
        try {
            return retry(action);
        } catch (DataIntegrityViolationException ex) {
            return recoverIdempotency(key, hash, ex);
        }
    }

    private JournalResult retry(Supplier<JournalResult> action) {
        int attempt = 0;
        while (true) {
            try {
                JournalResult result = action.get();
                if (result == null) {
                    throw new IllegalStateException("Ledger transaction returned no result");
                }
                return result;
            } catch (RuntimeException ex) {
                if (!isOptimistic(ex)) {
                    throw ex;
                }
                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    throw new LedgerException.ConcurrentUpdate(
                            "The account was updated concurrently. Retry the request.");
                }
                log.debug("Retrying ledger write after optimistic lock, attempt {}", attempt);
                sleep(attempt);
            }
        }
    }

    private JournalResult postDeposit(UUID accountId, BigDecimal amount, String description, String key, String hash) {
        return tx.execute(status -> {
            Optional<JournalEntry> existing = journals.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                return replay(existing.get(), hash);
            }
            Account customer = requireAccount(accountId);
            String currency = customer.getCurrency();
            Account cash = accounts.findFirstByAccountTypeAndCurrency(AccountType.ASSET, currency)
                    .orElseThrow(() -> new IllegalStateException(
                            "Internal cash account for " + currency + " is not configured"));
            Map<UUID, Account> loaded = loadInIdOrder(cash.getId(), customer.getId());
            Account cashAccount = loaded.get(cash.getId());
            Account customerAccount = loaded.get(customer.getId());
            TransferRules.validateDeposit(cashAccount.snapshot(), customerAccount.snapshot(), amount);
            List<Posting> postings = TransferRules.depositPostings(
                    cashAccount.getId(), customerAccount.getId(), amount, customerAccount.getCurrency());
            return commit(JournalType.DEPOSIT, description, key, hash, postings, loaded);
        });
    }

    private JournalResult postTransfer(
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            String description,
            String key,
            String hash) {
        return tx.execute(status -> {
            Optional<JournalEntry> existing = journals.findByIdempotencyKey(key);
            if (existing.isPresent()) {
                return replay(existing.get(), hash);
            }
            Map<UUID, Account> loaded = loadInIdOrder(fromAccountId, toAccountId);
            Account from = loaded.get(fromAccountId);
            Account to = loaded.get(toAccountId);
            TransferRules.validateTransfer(from.snapshot(), to.snapshot(), amount);
            List<Posting> postings = TransferRules.transferPostings(
                    from.getId(), to.getId(), amount, from.getCurrency());
            return commit(JournalType.TRANSFER, description, key, hash, postings, loaded);
        });
    }

    private JournalResult commit(
            JournalType type,
            String description,
            String key,
            String hash,
            List<Posting> postings,
            Map<UUID, Account> loaded) {
        TransferRules.assertBalanced(postings);
        for (Posting posting : postings) {
            loaded.get(posting.accountId()).apply(posting.direction(), posting.amount());
        }
        accounts.saveAll(loaded.values());
        JournalEntry entry = JournalEntry.record(type, description, key, hash, postings, loaded);
        journals.saveAndFlush(entry);
        return new JournalResult(JournalMapper.toResponse(entry), false);
    }

    private JournalResult replay(JournalEntry existing, String hash) {
        if (!existing.getRequestHash().equals(hash)) {
            throw new LedgerException.IdempotencyConflict(
                    "This Idempotency-Key was already used for a different request");
        }
        return new JournalResult(JournalMapper.toResponse(existing), true);
    }

    private JournalResult recoverIdempotency(String key, String hash, DataIntegrityViolationException ex) {
        JournalResult recovered = tx.execute(status -> journals.findByIdempotencyKey(key)
                .map(entry -> replay(entry, hash))
                .orElse(null));
        if (recovered == null) {
            throw ex;
        }
        return recovered;
    }

    private Map<UUID, Account> loadInIdOrder(UUID left, UUID right) {
        List<UUID> ordered = left.compareTo(right) <= 0 ? List.of(left, right) : List.of(right, left);
        Map<UUID, Account> loaded = new LinkedHashMap<>();
        for (UUID id : ordered) {
            loaded.putIfAbsent(id, requireAccount(id));
        }
        if (!loaded.containsKey(left) || !loaded.containsKey(right)) {
            throw new LedgerException.NotFound("Account was not found");
        }
        return loaded;
    }

    private Account requireAccount(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new LedgerException.NotFound("Account " + id + " was not found"));
    }

    private static String text(String description, String fallback) {
        if (description == null || description.isBlank()) {
            return fallback;
        }
        return description.trim();
    }

    private static boolean isOptimistic(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof OptimisticLockingFailureException || current instanceof OptimisticLockException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void sleep(int attempt) {
        try {
            Thread.sleep(10L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LedgerException.ConcurrentUpdate("Interrupted while retrying a concurrent update");
        }
    }
}

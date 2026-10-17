package com.arpanbom.ledger.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arpanbom.ledger.account.AccountResponse;
import com.arpanbom.ledger.account.AccountService;
import com.arpanbom.ledger.account.CreateAccountRequest;
import com.arpanbom.ledger.customer.CreateCustomerRequest;
import com.arpanbom.ledger.customer.CustomerService;
import com.arpanbom.ledger.domain.LedgerException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceTest {

    @Autowired
    private CustomerService customers;

    @Autowired
    private AccountService accounts;

    @Autowired
    private LedgerService ledger;

    @Test
    void depositAndTransferKeepBalancedEntries() {
        AccountPair pair = openPair();
        JournalResult deposit = ledger.deposit(pair.from().id(), new BigDecimal("100.00"), "Opening", key());

        assertThat(deposit.replay()).isFalse();
        assertThat(deposit.body().type()).isEqualTo(JournalType.DEPOSIT);
        assertBalanced(deposit.body());

        JournalResult transfer = ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("40.50"), "Invoice", key());

        assertBalanced(transfer.body());
        assertThat(transfer.body().lines()).extracting(JournalLineResponse::direction)
                .containsExactly(EntryDirection.DEBIT, EntryDirection.CREDIT);
        assertThat(accounts.get(pair.from().id()).balance()).isEqualByComparingTo("59.50");
        assertThat(accounts.get(pair.to().id()).balance()).isEqualByComparingTo("40.50");
        assertThat(ledger.entries(pair.from().id())).hasSize(2);
    }

    @Test
    void replayReturnsTheOriginalJournalWithoutPostingTwice() {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("30.00"), "Opening", key());
        String transferKey = key();

        JournalResult first = ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("10.5"), "Rent", transferKey);
        JournalResult second = ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("10.50"), "Rent", transferKey);

        assertThat(second.replay()).isTrue();
        assertThat(second.body().id()).isEqualTo(first.body().id());
        assertThat(accounts.get(pair.from().id()).balance()).isEqualByComparingTo("19.50");
        assertThat(accounts.get(pair.to().id()).balance()).isEqualByComparingTo("10.50");
    }

    @Test
    void sameKeyWithADifferentBodyConflicts() {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("30.00"), "Opening", key());
        String transferKey = key();
        ledger.transfer(pair.from().id(), pair.to().id(), new BigDecimal("5.00"), "Rent", transferKey);

        assertThatThrownBy(() -> ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("6.00"), "Rent", transferKey))
                .isInstanceOf(LedgerException.IdempotencyConflict.class);
        assertThat(accounts.get(pair.from().id()).balance()).isEqualByComparingTo("25.00");
    }

    @Test
    void insufficientFundsLeavesBalancesUnchanged() {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("10.00"), "Opening", key());

        assertThatThrownBy(() -> ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("10.01"), "Too much", key()))
                .isInstanceOf(LedgerException.InsufficientFunds.class);
        assertThat(accounts.get(pair.from().id()).balance()).isEqualByComparingTo("10.00");
        assertThat(accounts.get(pair.to().id()).balance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transferToTheSameAccountIsRejected() {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("10.00"), "Opening", key());

        assertThatThrownBy(() -> ledger.transfer(
                pair.from().id(), pair.from().id(), new BigDecimal("1.00"), "Loop", key()))
                .isInstanceOf(LedgerException.SameAccount.class);
    }

    @Test
    void concurrentTransfersDoNotLoseUpdates() throws Exception {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("100.00"), "Opening", key());
        int threads = 10;
        runTogether(threads, index -> ledger.transfer(
                pair.from().id(), pair.to().id(), new BigDecimal("10.00"), "Part " + index, key()));

        AccountResponse from = accounts.get(pair.from().id());
        AccountResponse to = accounts.get(pair.to().id());
        assertThat(from.balance()).isEqualByComparingTo("0.00");
        assertThat(to.balance()).isEqualByComparingTo("100.00");
        assertThat(from.version()).isEqualTo(11L);
        assertThat(to.version()).isEqualTo(10L);
        assertThat(ledger.entries(pair.from().id())).hasSize(11);
    }

    @Test
    void concurrentSpendOfTheFullBalanceAllowsOnlyOneDebit() throws Exception {
        AccountPair pair = openPair();
        ledger.deposit(pair.from().id(), new BigDecimal("50.00"), "Opening", key());

        List<Integer> outcomes = runTogether(2, index -> {
            try {
                ledger.transfer(pair.from().id(), pair.to().id(), new BigDecimal("50.00"), "All", key());
                return 1;
            } catch (LedgerException.InsufficientFunds ex) {
                return 0;
            }
        });

        assertThat(outcomes.stream().mapToInt(Integer::intValue).sum()).isEqualTo(1);
        assertThat(accounts.get(pair.from().id()).balance()).isEqualByComparingTo("0.00");
        assertThat(accounts.get(pair.to().id()).balance()).isEqualByComparingTo("50.00");
    }

    private AccountPair openPair() {
        var customer = customers.create(new CreateCustomerRequest("Ada Lovelace", email()));
        AccountResponse from = accounts.open(new CreateAccountRequest(customer.id(), "USD"));
        AccountResponse to = accounts.open(new CreateAccountRequest(customer.id(), "USD"));
        return new AccountPair(from, to);
    }

    private static void assertBalanced(JournalResponse journal) {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (JournalLineResponse line : journal.lines()) {
            switch (line.direction()) {
                case DEBIT -> debits = debits.add(line.amount());
                case CREDIT -> credits = credits.add(line.amount());
            }
        }
        assertThat(debits).isEqualByComparingTo(credits);
        assertThat(journal.lines()).hasSizeGreaterThanOrEqualTo(2);
    }

    private static <T> List<T> runTogether(int threads, Work<T> work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < threads; index++) {
                int current = index;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start");
                    }
                    return work.run(current);
                }));
            }
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Workers did not start");
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            return results;
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String key() {
        return "key-" + UUID.randomUUID();
    }

    private static String email() {
        return "ada-" + UUID.randomUUID() + "@example.com";
    }

    @FunctionalInterface
    private interface Work<T> {
        T run(int index) throws Exception;
    }

    private record AccountPair(AccountResponse from, AccountResponse to) {
    }
}

package com.arpanbom.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arpanbom.ledger.account.AccountStatus;
import com.arpanbom.ledger.account.AccountType;
import com.arpanbom.ledger.journal.EntryDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferRulesTest {

    private final UUID fromId = UUID.randomUUID();
    private final UUID toId = UUID.randomUUID();
    private final UUID cashId = UUID.randomUUID();

    @Test
    void normalizesScaleAndRejectsUnusableAmounts() {
        assertThat(TransferRules.normalizeAmount(new BigDecimal("10.5"))).isEqualTo(new BigDecimal("10.50"));
        assertThat(TransferRules.normalizeAmount(new BigDecimal("10.500"))).isEqualTo(new BigDecimal("10.50"));
        assertThat(TransferRules.normalizeAmount(new BigDecimal("0.01"))).isEqualTo(new BigDecimal("0.01"));

        assertThatThrownBy(() -> TransferRules.normalizeAmount(null))
                .isInstanceOf(LedgerException.InvalidAmount.class);
        assertThatThrownBy(() -> TransferRules.normalizeAmount(BigDecimal.ZERO))
                .isInstanceOf(LedgerException.InvalidAmount.class);
        assertThatThrownBy(() -> TransferRules.normalizeAmount(new BigDecimal("-1.00")))
                .isInstanceOf(LedgerException.InvalidAmount.class);
        assertThatThrownBy(() -> TransferRules.normalizeAmount(new BigDecimal("0.001")))
                .isInstanceOf(LedgerException.InvalidAmount.class);
        assertThatThrownBy(() -> TransferRules.normalizeAmount(new BigDecimal("100000000000000000.00")))
                .isInstanceOf(LedgerException.InvalidAmount.class);
    }

    @Test
    void transferPostingsDebitTheSenderAndCreditTheReceiver() {
        List<Posting> postings = TransferRules.transferPostings(fromId, toId, new BigDecimal("25.50"), "USD");

        TransferRules.assertBalanced(postings);
        assertThat(postings).containsExactly(
                new Posting(fromId, EntryDirection.DEBIT, new BigDecimal("25.50"), "USD"),
                new Posting(toId, EntryDirection.CREDIT, new BigDecimal("25.50"), "USD"));
    }

    @Test
    void depositPostingsDebitCashAndCreditTheCustomer() {
        List<Posting> postings = TransferRules.depositPostings(cashId, toId, new BigDecimal("40"), "USD");

        TransferRules.assertBalanced(postings);
        assertThat(postings).containsExactly(
                new Posting(cashId, EntryDirection.DEBIT, new BigDecimal("40.00"), "USD"),
                new Posting(toId, EntryDirection.CREDIT, new BigDecimal("40.00"), "USD"));
    }

    @Test
    void rejectsUnbalancedJournals() {
        List<Posting> postings = List.of(
                new Posting(fromId, EntryDirection.DEBIT, new BigDecimal("10.00"), "USD"),
                new Posting(toId, EntryDirection.CREDIT, new BigDecimal("9.00"), "USD"));

        assertThatThrownBy(() -> TransferRules.assertBalanced(postings))
                .isInstanceOf(LedgerException.UnbalancedJournal.class);
    }

    @Test
    void rejectsSameAccountCurrencyMismatchClosedAccountsAndInsufficientFunds() {
        AccountSnapshot open = snapshot(fromId, AccountStatus.OPEN, "USD", "50.00");
        AccountSnapshot other = snapshot(toId, AccountStatus.OPEN, "USD", "0.00");

        assertThatThrownBy(() -> TransferRules.validateTransfer(open, open, new BigDecimal("1.00")))
                .isInstanceOf(LedgerException.SameAccount.class);
        assertThatThrownBy(() -> TransferRules.validateTransfer(
                open, snapshot(toId, AccountStatus.OPEN, "EUR", "0.00"), new BigDecimal("1.00")))
                .isInstanceOf(LedgerException.CurrencyMismatch.class);
        assertThatThrownBy(() -> TransferRules.validateTransfer(
                snapshot(fromId, AccountStatus.FROZEN, "USD", "50.00"), other, new BigDecimal("1.00")))
                .isInstanceOf(LedgerException.AccountNotOpen.class);
        assertThatThrownBy(() -> TransferRules.validateTransfer(
                open, snapshot(toId, AccountStatus.CLOSED, "USD", "0.00"), new BigDecimal("1.00")))
                .isInstanceOf(LedgerException.AccountNotOpen.class);
        assertThatThrownBy(() -> TransferRules.validateTransfer(open, other, new BigDecimal("50.01")))
                .isInstanceOf(LedgerException.InsufficientFunds.class);
    }

    @Test
    void acceptsACoveredTransferBetweenOpenUsdAccounts() {
        TransferRules.validateTransfer(
                snapshot(fromId, AccountStatus.OPEN, "USD", "50.00"),
                snapshot(toId, AccountStatus.OPEN, "USD", "0.00"),
                new BigDecimal("50.00"));
    }

    @Test
    void depositRequiresCashAssetAndOpenCustomerLiability() {
        AccountSnapshot cash = new AccountSnapshot(
                cashId, "INTERNAL-CASH-USD", AccountType.ASSET, AccountStatus.OPEN, "USD", new BigDecimal("0.00"));
        AccountSnapshot customer = snapshot(toId, AccountStatus.OPEN, "USD", "0.00");

        TransferRules.validateDeposit(cash, customer, new BigDecimal("15.00"));

        assertThatThrownBy(() -> TransferRules.validateDeposit(customer, customer, new BigDecimal("15.00")))
                .isInstanceOf(LedgerException.UnsupportedCurrency.class);
        assertThatThrownBy(() -> TransferRules.validateDeposit(
                cash, snapshot(toId, AccountStatus.FROZEN, "USD", "0.00"), new BigDecimal("15.00")))
                .isInstanceOf(LedgerException.AccountNotOpen.class);
    }

    private static AccountSnapshot snapshot(UUID id, AccountStatus status, String currency, String balance) {
        return new AccountSnapshot(
                id, "LDG-" + id.toString().substring(0, 8), AccountType.LIABILITY, status, currency, new BigDecimal(balance));
    }
}

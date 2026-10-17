package com.arpanbom.ledger.domain;

import com.arpanbom.ledger.account.AccountStatus;
import com.arpanbom.ledger.account.AccountType;
import com.arpanbom.ledger.journal.EntryDirection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public final class TransferRules {

    public static final String SUPPORTED_CURRENCY = "USD";
    public static final int SCALE = 2;
    private static final int MAX_INTEGER_DIGITS = 17;

    private TransferRules() {
    }

    public static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new LedgerException.InvalidAmount("Amount is required");
        }
        BigDecimal normalized;
        try {
            normalized = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new LedgerException.InvalidAmount("Amount must be positive with at most 2 decimal places");
        }
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new LedgerException.InvalidAmount("Amount must be positive with at most 2 decimal places");
        }
        if (normalized.precision() - normalized.scale() > MAX_INTEGER_DIGITS) {
            throw new LedgerException.InvalidAmount("Amount exceeds the supported range");
        }
        return normalized;
    }

    public static void validateTransfer(AccountSnapshot from, AccountSnapshot to, BigDecimal amount) {
        BigDecimal normalized = normalizeAmount(amount);
        if (from.id().equals(to.id())) {
            throw new LedgerException.SameAccount("Transfers require two different accounts");
        }
        requireOpen(from);
        requireOpen(to);
        requireUsd(from.currency());
        if (!from.currency().equals(to.currency())) {
            throw new LedgerException.CurrencyMismatch("Both accounts must use the same currency");
        }
        if (from.type() != AccountType.LIABILITY || to.type() != AccountType.LIABILITY) {
            throw new LedgerException.SameAccount("Transfers are only allowed between customer accounts");
        }
        if (from.balance().compareTo(normalized) < 0) {
            throw new LedgerException.InsufficientFunds(
                    "Account " + from.accountNumber() + " does not have enough available balance");
        }
    }

    public static void validateDeposit(AccountSnapshot cash, AccountSnapshot customer, BigDecimal amount) {
        normalizeAmount(amount);
        requireOpen(cash);
        requireOpen(customer);
        requireUsd(customer.currency());
        if (cash.type() != AccountType.ASSET || customer.type() != AccountType.LIABILITY) {
            throw new LedgerException.UnsupportedCurrency("Deposits must credit a customer account from cash");
        }
        if (!cash.currency().equals(customer.currency())) {
            throw new LedgerException.CurrencyMismatch("Cash and customer accounts must use the same currency");
        }
    }

    public static List<Posting> transferPostings(UUID fromAccountId, UUID toAccountId, BigDecimal amount, String currency) {
        BigDecimal normalized = normalizeAmount(amount);
        return List.of(
                new Posting(fromAccountId, EntryDirection.DEBIT, normalized, currency),
                new Posting(toAccountId, EntryDirection.CREDIT, normalized, currency));
    }

    public static List<Posting> depositPostings(UUID cashAccountId, UUID customerAccountId, BigDecimal amount, String currency) {
        BigDecimal normalized = normalizeAmount(amount);
        return List.of(
                new Posting(cashAccountId, EntryDirection.DEBIT, normalized, currency),
                new Posting(customerAccountId, EntryDirection.CREDIT, normalized, currency));
    }

    public static void assertBalanced(List<Posting> postings) {
        if (postings == null || postings.size() < 2) {
            throw new LedgerException.UnbalancedJournal("A journal needs at least two lines");
        }
        BigDecimal debits = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal credits = BigDecimal.ZERO.setScale(SCALE);
        String currency = postings.get(0).currency();
        for (Posting posting : postings) {
            if (!currency.equals(posting.currency())) {
                throw new LedgerException.UnbalancedJournal("Journal lines must share one currency");
            }
            switch (posting.direction()) {
                case DEBIT -> debits = debits.add(posting.amount());
                case CREDIT -> credits = credits.add(posting.amount());
            }
        }
        if (debits.compareTo(credits) != 0) {
            throw new LedgerException.UnbalancedJournal("Debits must equal credits");
        }
    }

    private static void requireOpen(AccountSnapshot account) {
        if (account.status() != AccountStatus.OPEN) {
            throw new LedgerException.AccountNotOpen(
                    "Account " + account.accountNumber() + " is " + account.status());
        }
    }

    private static void requireUsd(String currency) {
        if (!SUPPORTED_CURRENCY.equals(currency)) {
            throw new LedgerException.UnsupportedCurrency("Only USD is supported");
        }
    }
}

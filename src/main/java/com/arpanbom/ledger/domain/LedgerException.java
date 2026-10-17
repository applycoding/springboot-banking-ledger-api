package com.arpanbom.ledger.domain;

public abstract sealed class LedgerException extends RuntimeException
        permits LedgerException.NotFound,
                LedgerException.InvalidAmount,
                LedgerException.InsufficientFunds,
                LedgerException.AccountNotOpen,
                LedgerException.SameAccount,
                LedgerException.CurrencyMismatch,
                LedgerException.UnsupportedCurrency,
                LedgerException.IdempotencyConflict,
                LedgerException.ConcurrentUpdate,
                LedgerException.DuplicateEmail,
                LedgerException.InvalidIdempotencyKey,
                LedgerException.UnbalancedJournal {

    private LedgerException(String message) {
        super(message);
    }

    public abstract String code();

    public abstract String title();

    public static final class NotFound extends LedgerException {
        public NotFound(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "not_found";
        }

        @Override
        public String title() {
            return "Not Found";
        }
    }

    public static final class InvalidAmount extends LedgerException {
        public InvalidAmount(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "invalid_amount";
        }

        @Override
        public String title() {
            return "Invalid Amount";
        }
    }

    public static final class InsufficientFunds extends LedgerException {
        public InsufficientFunds(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "insufficient_funds";
        }

        @Override
        public String title() {
            return "Insufficient Funds";
        }
    }

    public static final class AccountNotOpen extends LedgerException {
        public AccountNotOpen(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "account_not_open";
        }

        @Override
        public String title() {
            return "Account Not Open";
        }
    }

    public static final class SameAccount extends LedgerException {
        public SameAccount(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "same_account";
        }

        @Override
        public String title() {
            return "Same Account";
        }
    }

    public static final class CurrencyMismatch extends LedgerException {
        public CurrencyMismatch(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "currency_mismatch";
        }

        @Override
        public String title() {
            return "Currency Mismatch";
        }
    }

    public static final class UnsupportedCurrency extends LedgerException {
        public UnsupportedCurrency(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "unsupported_currency";
        }

        @Override
        public String title() {
            return "Unsupported Currency";
        }
    }

    public static final class IdempotencyConflict extends LedgerException {
        public IdempotencyConflict(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "idempotency_conflict";
        }

        @Override
        public String title() {
            return "Idempotency Conflict";
        }
    }

    public static final class ConcurrentUpdate extends LedgerException {
        public ConcurrentUpdate(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "concurrent_update";
        }

        @Override
        public String title() {
            return "Concurrent Update";
        }
    }

    public static final class DuplicateEmail extends LedgerException {
        public DuplicateEmail(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "duplicate_email";
        }

        @Override
        public String title() {
            return "Duplicate Email";
        }
    }

    public static final class InvalidIdempotencyKey extends LedgerException {
        public InvalidIdempotencyKey(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "invalid_idempotency_key";
        }

        @Override
        public String title() {
            return "Invalid Idempotency Key";
        }
    }

    public static final class UnbalancedJournal extends LedgerException {
        public UnbalancedJournal(String message) {
            super(message);
        }

        @Override
        public String code() {
            return "unbalanced_journal";
        }

        @Override
        public String title() {
            return "Unbalanced Journal";
        }
    }
}

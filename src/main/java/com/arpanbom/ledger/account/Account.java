package com.arpanbom.ledger.account;

import com.arpanbom.ledger.customer.Customer;
import com.arpanbom.ledger.domain.AccountSnapshot;
import com.arpanbom.ledger.domain.LedgerException;
import com.arpanbom.ledger.journal.EntryDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Account() {
    }

    public static Account openLiability(Customer customer, String accountNumber, String currency) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.customer = customer;
        account.accountNumber = accountNumber;
        account.accountType = AccountType.LIABILITY;
        account.status = AccountStatus.OPEN;
        account.currency = currency;
        account.balance = new BigDecimal("0.00");
        account.createdAt = LocalDateTime.now();
        return account;
    }

    public static Account openAsset(String accountNumber, String currency, BigDecimal balance) {
        Account account = new Account();
        account.id = UUID.randomUUID();
        account.accountNumber = accountNumber;
        account.accountType = AccountType.ASSET;
        account.status = AccountStatus.OPEN;
        account.currency = currency;
        account.balance = balance;
        account.createdAt = LocalDateTime.now();
        return account;
    }

    public void apply(EntryDirection direction, BigDecimal amount) {
        boolean increases = switch (accountType) {
            case ASSET -> direction == EntryDirection.DEBIT;
            case LIABILITY -> direction == EntryDirection.CREDIT;
        };
        BigDecimal next = increases ? balance.add(amount) : balance.subtract(amount);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new LedgerException.InsufficientFunds(
                    "Account " + accountNumber + " does not have enough available balance");
        }
        this.balance = next;
    }

    public AccountSnapshot snapshot() {
        return new AccountSnapshot(id, accountNumber, accountType, status, currency, balance);
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}

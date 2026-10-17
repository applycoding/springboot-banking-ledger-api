package com.arpanbom.ledger.account;

import com.arpanbom.ledger.customer.Customer;
import com.arpanbom.ledger.customer.CustomerService;
import com.arpanbom.ledger.domain.LedgerException;
import com.arpanbom.ledger.domain.TransferRules;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final int NUMBER_ATTEMPTS = 5;

    private final AccountRepository accounts;
    private final CustomerService customers;

    public AccountService(AccountRepository accounts, CustomerService customers) {
        this.accounts = accounts;
        this.customers = customers;
    }

    @Transactional
    public AccountResponse open(CreateAccountRequest request) {
        if (!TransferRules.SUPPORTED_CURRENCY.equals(request.currency())) {
            throw new LedgerException.UnsupportedCurrency("Only USD is supported");
        }
        Customer customer = customers.require(request.customerId());
        Account account = Account.openLiability(customer, nextAccountNumber(), request.currency());
        return toResponse(accounts.saveAndFlush(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listForCustomer(UUID customerId) {
        customers.require(customerId);
        return accounts.findByCustomer_IdOrderByCreatedAtAsc(customerId).stream()
                .map(AccountService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Account require(UUID id) {
        return accounts.findById(id)
                .orElseThrow(() -> new LedgerException.NotFound("Account " + id + " was not found"));
    }

    private String nextAccountNumber() {
        for (int attempt = 0; attempt < NUMBER_ATTEMPTS; attempt++) {
            String candidate = AccountNumbers.next();
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate an account number");
    }

    static AccountResponse toResponse(Account account) {
        UUID customerId = account.getCustomer() == null ? null : account.getCustomer().getId();
        return new AccountResponse(
                account.getId(),
                customerId,
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance(),
                account.getVersion() == null ? 0L : account.getVersion(),
                account.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}

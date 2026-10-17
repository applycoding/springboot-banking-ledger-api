package com.arpanbom.ledger.account;

import com.arpanbom.ledger.journal.AccountEntryResponse;
import com.arpanbom.ledger.journal.JournalResponse;
import com.arpanbom.ledger.journal.JournalResult;
import com.arpanbom.ledger.journal.LedgerService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accounts;
    private final LedgerService ledger;

    public AccountController(AccountService accounts, LedgerService ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> open(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse body = accounts.open(request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + body.id())).body(body);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return accounts.get(id);
    }

    @GetMapping("/{id}/entries")
    public List<AccountEntryResponse> entries(@PathVariable UUID id) {
        return ledger.entries(id);
    }

    @PostMapping("/{id}/deposits")
    public ResponseEntity<JournalResponse> deposit(
            @PathVariable UUID id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        JournalResult result = ledger.deposit(id, request.amount(), request.description(), idempotencyKey);
        if (result.replay()) {
            return ResponseEntity.ok(result.body());
        }
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + result.body().id())).body(result.body());
    }
}

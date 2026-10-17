package com.arpanbom.ledger.journal;

import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final LedgerService ledger;

    public TransferController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping
    public ResponseEntity<JournalResponse> transfer(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        JournalResult result = ledger.transfer(
                request.fromAccountId(),
                request.toAccountId(),
                request.amount(),
                request.description(),
                idempotencyKey);
        if (result.replay()) {
            return ResponseEntity.ok(result.body());
        }
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + result.body().id())).body(result.body());
    }

    @GetMapping("/{id}")
    public JournalResponse get(@PathVariable UUID id) {
        return ledger.get(id);
    }
}

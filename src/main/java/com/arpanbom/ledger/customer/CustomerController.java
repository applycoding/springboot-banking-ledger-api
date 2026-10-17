package com.arpanbom.ledger.customer;

import com.arpanbom.ledger.account.AccountResponse;
import com.arpanbom.ledger.account.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customers;
    private final AccountService accounts;

    public CustomerController(CustomerService customers, AccountService accounts) {
        this.customers = customers;
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse body = customers.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + body.id())).body(body);
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return customers.list();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id) {
        return customers.get(id);
    }

    @GetMapping("/{id}/accounts")
    public List<AccountResponse> accounts(@PathVariable UUID id) {
        return accounts.listForCustomer(id);
    }
}

package com.arpanbom.ledger.customer;

import com.arpanbom.ledger.domain.LedgerException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (customers.findByEmailIgnoreCase(email).isPresent()) {
            throw new LedgerException.DuplicateEmail("A customer with email " + email + " already exists");
        }
        try {
            return toResponse(customers.saveAndFlush(Customer.create(name, email)));
        } catch (DataIntegrityViolationException ex) {
            throw new LedgerException.DuplicateEmail("A customer with email " + email + " already exists");
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list() {
        return customers.findAll().stream().map(CustomerService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Customer require(UUID id) {
        return customers.findById(id)
                .orElseThrow(() -> new LedgerException.NotFound("Customer " + id + " was not found"));
    }

    private static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
}

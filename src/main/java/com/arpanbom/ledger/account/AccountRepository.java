package com.arpanbom.ledger.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomer_IdOrderByCreatedAtAsc(UUID customerId);

    Optional<Account> findFirstByAccountTypeAndCurrency(AccountType accountType, String currency);
}

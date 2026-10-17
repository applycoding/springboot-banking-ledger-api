package com.arpanbom.ledger.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arpanbom.ledger.customer.Customer;
import com.arpanbom.ledger.domain.LedgerException;
import com.arpanbom.ledger.journal.EntryDirection;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AccountBalanceTest {

    @Test
    void liabilityCreditIncreasesAndDebitDecreases() {
        Account account = customerAccount();

        account.apply(EntryDirection.CREDIT, new BigDecimal("10.00"));
        account.apply(EntryDirection.DEBIT, new BigDecimal("3.25"));

        assertThat(account.getBalance()).isEqualByComparingTo("6.75");
    }

    @Test
    void assetDebitIncreasesAndCreditDecreases() {
        Account cash = Account.openAsset("INTERNAL-CASH-USD", "USD", new BigDecimal("0.00"));

        cash.apply(EntryDirection.DEBIT, new BigDecimal("20.00"));
        cash.apply(EntryDirection.CREDIT, new BigDecimal("5.00"));

        assertThat(cash.getBalance()).isEqualByComparingTo("15.00");
    }

    @Test
    void rejectsADebitThatWouldMakeTheBalanceNegative() {
        Account account = customerAccount();
        account.apply(EntryDirection.CREDIT, new BigDecimal("8.00"));

        assertThatThrownBy(() -> account.apply(EntryDirection.DEBIT, new BigDecimal("8.01")))
                .isInstanceOf(LedgerException.InsufficientFunds.class);
        assertThat(account.getBalance()).isEqualByComparingTo("8.00");
    }

    private static Account customerAccount() {
        return Account.openLiability(Customer.create("Ada Lovelace", "ada@example.com"), "LDG0000000001", "USD");
    }
}

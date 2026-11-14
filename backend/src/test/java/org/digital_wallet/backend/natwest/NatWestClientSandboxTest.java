package org.digital_wallet.backend.natwest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("natwest-sandbox")
@EnabledIfEnvironmentVariable(named = "NATWEST_INTEGRATION", matches = "true")
class NatWestClientSandboxTest {

    private final NatWestClient client;

    @Autowired
    public NatWestClientSandboxTest(NatWestClient natWestClient) {
        client = natWestClient;
    }

    @Test
    void fetchesAccountsAndTransactionsForTheFirstAccount() {
        List<NatWestAccount> accounts = client.fetchAccounts();

        assertThat(accounts).isNotEmpty().allSatisfy(account -> {
            assertThat(account.accountId()).isNotBlank();
            assertThat(account.currency()).isEqualTo("GBP");
        });

        String firstAccountId = accounts.get(0).accountId();
        List<NatWestTransaction> transactions = client.fetchTransactions(firstAccountId);

        assertThat(transactions).allSatisfy(transaction -> {
            assertThat(transaction.accountId()).isEqualTo(firstAccountId);
            assertThat(transaction.transactionId()).isNotBlank();
            assertThat(transaction.amount()).isNotBlank();
            assertThat(transaction.currency()).isEqualTo("GBP");
        });
    }
}

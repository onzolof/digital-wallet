package org.digital_wallet.backend.api;

import org.digital_wallet.backend.api.model.Account;
import org.digital_wallet.backend.api.model.Transaction;
import org.digital_wallet.backend.utils.CalculationUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class MockDataGenerator {

    private static final String CURRENCY = "CHF";
    private static final String[] PAYEES = {
            "Lohn",
            "Miete",
            "Migros",
            "Coop",
            "SBB",
            "Swisscom",
            "Denner",
            "Krankenkasse",
            "Restaurant Zeughauskeller",
            "Kreditkartenabrechnung"
    };

    public List<Account> append(List<Account> accounts) {
        String userId = accounts.stream()
                .map(Account::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse("mock-user");
        List<Account> enhanced = new ArrayList<>(accounts);
        enhanced.add(createAccount(userId, "Zürcher Kantonalbank", "zkb-1001", "Savings", 15));
        enhanced.add(createAccount(userId, "St.Galler Kantonalbank", "sgkb-1001", "Salary", 12));
        enhanced.add(createAccount(userId, "Clientis", "clientis-1001", "Retirement", 18));
        return enhanced;
    }

    private static Account createAccount(String userId, String bank, String accountId, String accountCategory, int transactionCount) {
        return new Account(
                userId,
                bank,
                accountId,
                CURRENCY,
                "Retail",
                accountCategory,
                null,
                null,
                createTransactions(accountId, transactionCount));
    }

    private static List<Transaction> createTransactions(String accountId, int count) {
        OffsetDateTime bookedAt = OffsetDateTime.parse("2026-11-01T08:00:00Z");
        List<BigDecimal> signedAmounts = signedAmounts(accountId, count);
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BigDecimal signedAmount = signedAmounts.get(i);
            boolean credit = signedAmount.signum() < 0;
            BigDecimal amount = signedAmount.abs();
            transactions.add(new Transaction(
                    accountId,
                    accountId + "-" + (i + 1),
                    amount,
                    CalculationUtils.roundIfNeeded(CURRENCY, amount),
                    CURRENCY,
                    credit ? "Credit" : "Debit",
                    "Booked",
                    bookedAt.minusDays(i * 2L),
                    PAYEES[i % PAYEES.length]));
        }
        return transactions;
    }

    private static List<BigDecimal> signedAmounts(String accountId, int count) {
        long state = Integer.toUnsignedLong(accountId.hashCode()) * 0x9E3779B97F4A7C15L + 0xBF58476D1CE4E5B9L;
        List<BigDecimal> amounts = new ArrayList<>();
        while (amounts.size() < count) {
            state = state * 0xD1342543DE82EF95L + 0x5DEECE66DL;
            int step = (int) Long.remainderUnsigned(state, 40_001);
            BigDecimal amount = new BigDecimal("-1000.00")
                    .add(new BigDecimal("0.05").multiply(BigDecimal.valueOf(step)));
            if (!amounts.contains(amount)) {
                amounts.add(amount);
            }
        }
        return amounts;
    }
}

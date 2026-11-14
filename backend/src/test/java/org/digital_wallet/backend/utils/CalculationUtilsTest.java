package org.digital_wallet.backend.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.digital_wallet.backend.api.model.Account;
import org.digital_wallet.backend.api.model.Transaction;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CalculationUtilsTest {

    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("1.20");

    @ParameterizedTest(name = "{0} balance {2} converts to {3} CHF")
    @MethodSource("accountBalances")
    void recalculateAccountBalances(String currency, List<Transaction> transactions, String expectedBalance, String expectedChf) {
        Account account = account(currency, transactions);

        List<Account> result = CalculationUtils.recalculateAccountBalances(List.of(account), EXCHANGE_RATE);

        assertThat(result).containsExactly(account);
        assertThat(account.getTotalBalance()).isEqualByComparingTo(expectedBalance);
        assertThat(account.getTotalBalanceChf()).isEqualByComparingTo(expectedChf);
    }

    @ParameterizedTest(name = "{0} {1} rounds to {2}")
    @CsvSource({
            "CHF, 1.02, 1.00",
            "CHF, 1.03, 1.05",
            "CHF, 1.025, 1.05",
            "CHF, -1.025, -1.05",
            "CHF, 0, 0.00"
    })
    void roundIfNeededRoundsChfAmountsToTheNearestFiveRappen(String currency, String amount, String expected) {
        assertThat(CalculationUtils.roundIfNeeded(currency, new BigDecimal(amount))).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "{0} amounts stay unchanged")
    @ValueSource(strings = {"GBP", "EUR", "USD"})
    void roundIfNeededLeavesNonChfAmountsUntouched(String currency) {
        BigDecimal amount = new BigDecimal("10.02");

        assertThat(CalculationUtils.roundIfNeeded(currency, amount)).isSameAs(amount);
    }

    private static Stream<Arguments> accountBalances() {
        return Stream.of(
                arguments("GBP", List.of(transaction("Debit", "5.00"), transaction("Credit", "1.25")), "3.75", "4.50"),
                arguments("CHF", List.of(transaction("Debit", "10.02")), "10.00", "12.00"),
                arguments("GBP", List.of(transaction("Debit", "10.02")), "10.02", "12.00"),
                arguments("CHF", List.of(transaction("Credit", "1.03")), "-1.05", "-1.25"),
                arguments("EUR", List.of(), "0", "0"));
    }

    private static Account account(String currency, List<Transaction> transactions) {
        Account account = new Account();
        account.setCurrency(currency);
        account.setTransactions(transactions);
        return account;
    }

    private static Transaction transaction(String creditDebitIndicator, String amount) {
        Transaction transaction = new Transaction();
        transaction.setCreditDebitIndicator(creditDebitIndicator);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }
}

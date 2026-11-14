package org.digital_wallet.backend.utils;

import org.digital_wallet.backend.api.model.Account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CalculationUtils {

    public static List<Account> recalculateAccountBalances(List<Account> fetchedAccounts, BigDecimal exchangeRate) {
        return fetchedAccounts.stream()
                .peek(account -> {
                    BigDecimal totalBalance = account.getTransactions().stream()
                            .map(trx -> trx.getCreditDebitIndicator().equals("Credit") ? trx.getAmount().multiply(BigDecimal.ONE.negate()) : trx.getAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    totalBalance = roundIfNeeded(account.getCurrency(), totalBalance);
                    account.setTotalBalance(totalBalance);
                    account.setTotalBalanceChf(roundToFiveRappen(totalBalance.multiply(exchangeRate)));
                })
                .toList();
    }

    public static BigDecimal roundIfNeeded(String currency, BigDecimal amount) {
        if (currency.equals("CHF")) {
            // only apply rounding for CHF
            return roundToFiveRappen(amount);
        } else {
            return amount;
        }
    }

    private static BigDecimal roundToFiveRappen(BigDecimal amount) {
        BigDecimal increment = new BigDecimal("0.05");

        return amount.divide(increment, 0, RoundingMode.HALF_UP)
                .multiply(increment)
                .setScale(2);
    }

}

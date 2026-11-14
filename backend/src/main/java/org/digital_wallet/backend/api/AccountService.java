package org.digital_wallet.backend.api;

import org.digital_wallet.backend.api.model.Account;
import org.digital_wallet.backend.api.model.AccountOverview;
import org.digital_wallet.backend.api.model.MinimalAccount;
import org.digital_wallet.backend.api.model.Transaction;
import org.digital_wallet.backend.natwest.NatWestAccount;
import org.digital_wallet.backend.natwest.NatWestClient;
import org.digital_wallet.backend.natwest.NatWestTransaction;
import org.digital_wallet.backend.utils.CalculationUtils;
import org.digital_wallet.backend.utils.ExchangeRateClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

//    todo: write unit test

    private final NatWestClient natWestClient;
    private final MockDataGenerator mockDataGenerator;
    private final ExchangeRateClient exchangeRateClient;

    @Autowired
    public AccountService(NatWestClient natWestClient, MockDataGenerator mockDataGenerator, ExchangeRateClient exchangeRateClient) {
        this.natWestClient = natWestClient;
        this.mockDataGenerator = mockDataGenerator;
        this.exchangeRateClient = exchangeRateClient;
    }

    public Optional<AccountOverview> getAccountOverview() {
        List<MinimalAccount> accountList = fetchAccounts()
                .stream()
                .map(this::toMinimalAccount)
                .toList();
        if (accountList.isEmpty()) {
            return Optional.empty();
        } else {
            // derive the account owner from the first account (assume, all accounts belong to the same individual)
            MinimalAccount firstAccount = accountList.getFirst();
            BigDecimal overallBalance = accountList.stream()
                    .map(MinimalAccount::getTotalBalanceChf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return Optional.of(new AccountOverview(firstAccount.getUserId(), firstAccount.getUsername(), overallBalance, accountList));
        }
    }

    public Optional<Account> getAccount(String accountId) {
        return fetchAccounts().stream().filter(account -> accountId.equals(account.getAccountId())).findFirst();
    }

    private List<Account> fetchAccounts() {
        List<Account> fetchedAccounts = natWestClient.fetchAccounts()
                .stream()
                .map(natWestAccount -> {
                    List<Transaction> transactions = natWestClient.fetchTransactions(natWestAccount.accountId()).stream()
                            .map(AccountService::toTransaction)
                            .toList();
                    return toAccount(natWestAccount, transactions);
                })
                .toList();
        return CalculationUtils.recalculateAccountBalances(mockDataGenerator.append(fetchedAccounts), exchangeRateClient::fetchExchangeRate);
    }


    private MinimalAccount toMinimalAccount(Account account) {
        return new MinimalAccount(
                account.getUserId(),
                account.getUsername(),
                account.getAccountId(),
                account.getCurrency(),
                account.getAccountCategory(),
                account.getTotalBalance(),
                account.getTotalBalanceChf());
    }

    private Account toAccount(NatWestAccount account, List<Transaction> transactions) {
        return new Account(
                account.identification(),
                account.nickname(),
                account.accountId(),
                account.currency(),
                account.accountType(),
                account.accountCategory(),
                null,
                null,
                transactions);
    }

    private static Transaction toTransaction(NatWestTransaction transaction) {
        OffsetDateTime bookingDateTime = bookingDateTime(transaction.bookingDateTime());
        BigDecimal amount = new BigDecimal(transaction.amount());
        return new Transaction(
                transaction.accountId(),
                transaction.transactionId(),
                amount,
                CalculationUtils.roundIfNeeded(transaction.currency(), amount),
                transaction.currency(),
                transaction.creditDebitIndicator(),
                transaction.status(),
                bookingDateTime,
                transaction.transactionInformation());
    }

    private static OffsetDateTime bookingDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }

}

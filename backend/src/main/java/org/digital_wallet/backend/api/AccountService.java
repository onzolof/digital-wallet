package org.digital_wallet.backend.api;

import org.digital_wallet.backend.api.model.*;
import org.digital_wallet.backend.natwest.NatWestClient;
import org.digital_wallet.backend.natwest.NatWestTransaction;
import org.digital_wallet.backend.natwest.NatwestAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final NatWestClient natWestClient;

    @Autowired
    public AccountService(NatWestClient natWestClient) {
        this.natWestClient = natWestClient;
    }

    public AccountOverviewDto getAccountOverview() {
        List<AccountEntry> accounts = natWestClient.fetchAccounts().stream()
                .map(this::toAccountEntry)
                .toList();
        return new AccountOverviewDto(null, null, null, accounts);
    }

    public Optional<AccountDto> getAccount(String accountId) {
        return natWestClient.fetchAccounts().stream()
                .filter(account -> accountId.equals(account.accountId()))
                .findFirst()
                .map(natWestAccount -> {
                    List<Transaction> transactions = natWestClient.fetchTransactions(natWestAccount.accountId()).stream()
                            .map(AccountService::toTransaction)
                            .toList();
                    return toAccountDto(natWestAccount, transactions);
                });
    }

    private AccountEntry toAccountEntry(NatwestAccount account) {
        return new AccountEntry(
                account.accountId(),
                account.status(),
                account.currency(),
                account.accountType(),
                account.accountSubType(),
                null,
                null);
    }

    private AccountDto toAccountDto(NatwestAccount account, List<Transaction> transactions) {
        return new AccountDto(
                account.accountId(),
                account.status(),
                account.currency(),
                account.accountType(),
                account.accountSubType(),
                null,
                null,
                transactions);
    }

    private static Transaction toTransaction(NatWestTransaction transaction) {
        OffsetDateTime bookingDateTime = bookingDateTime(transaction.bookingDateTime());
        LocalDate bookingDate = bookingDateTime != null ? bookingDateTime.toLocalDate() : null;
        return new Transaction(
                transaction.accountId(),
                transaction.transactionId(),
                transaction.amount(),
                transaction.currency(),
                transaction.creditDebitIndicator(),
                transaction.status(),
                bookingDateTime,
                bookingDate,
                transaction.transactionInformation());
    }

    private static OffsetDateTime bookingDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value);
    }
}

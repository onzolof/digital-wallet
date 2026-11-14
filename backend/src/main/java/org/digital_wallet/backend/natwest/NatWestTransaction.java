package org.digital_wallet.backend.natwest;

public record NatWestTransaction(
        String accountId,
        String transactionId,
        String amount,
        String currency,
        String creditDebitIndicator,
        String status,
        String bookingDateTime,
        String transactionInformation
) {
}

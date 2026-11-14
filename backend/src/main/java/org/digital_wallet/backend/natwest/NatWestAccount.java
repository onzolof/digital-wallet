package org.digital_wallet.backend.natwest;

public record NatWestAccount(
		String accountId,
		String status,
		String currency,
		String accountType,
		String accountCategory,
		String nickname,
		String schemeName,
		String identification
) {
}

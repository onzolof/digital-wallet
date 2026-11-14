package org.digital_wallet.backend.natwest;

public record NatwestAccount(
		String accountId,
		String status,
		String currency,
		String accountType,
		String accountSubType,
		String nickname,
		String schemeName,
		String identification
) {
}

package org.digital_wallet.backend.natwest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "natwest")
public record NatwestProperties(
		String wellKnownEndpoint,
		String apiUrlPrefix,
		String clientId,
		String clientSecret,
		String redirectUrl,
		String psuUsername
) {
}

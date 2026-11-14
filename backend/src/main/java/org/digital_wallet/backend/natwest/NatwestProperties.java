package org.digital_wallet.backend.natwest;

public record NatwestProperties(
        String wellKnownEndpoint,
        String apiUrlPrefix,
        String clientId,
        String clientSecret,
        String redirectUrl,
        String psuUsername
) {
}

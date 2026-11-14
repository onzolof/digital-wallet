package org.digital_wallet.backend;

import org.digital_wallet.backend.natwest.NatwestProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public NatwestProperties natwestProperties() {
        return new NatwestProperties(
                "https://api.sandbox.natwest.com/.well-known/openid-configuration",
                "https://ob.sandbox.natwest.com",
                // TODO: 14.11.2026 read from env var
                "H2qVDcxzwmUW1RabLFqOYaGwi0dxDULxc1vNOxyoGBE=",
                // TODO: 14.11.2026 read from env var
                "YGhQeeoZv8baiNxU5CfSagkcc44MliPQjjS_L5mlODQ=",
                "https://1ee36bc4-2a81-4917-8f1e-5a3c3728f94e.example.org/redirect",
                "123456789012@1ee36bc4-2a81-4917-8f1e-5a3c3728f94e.example.org"
        );
    }

}

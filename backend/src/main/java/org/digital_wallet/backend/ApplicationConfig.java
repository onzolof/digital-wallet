package org.digital_wallet.backend;

import org.digital_wallet.backend.natwest.NatwestProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

    private static final String CLIENT_ID_ENV_VAR = "CLIENT_ID";
    private static final String CLIENT_SECRET_ENV_VAR = "CLIENT_SECRET";

    private final Environment environment;

    @Autowired
    public ApplicationConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public NatwestProperties natwestProperties() {
        return new NatwestProperties(
                "https://api.sandbox.natwest.com/.well-known/openid-configuration",
                "https://ob.sandbox.natwest.com",
                environment.getProperty(CLIENT_ID_ENV_VAR),
                environment.getProperty(CLIENT_SECRET_ENV_VAR),
                "https://1ee36bc4-2a81-4917-8f1e-5a3c3728f94e.example.org/redirect",
                "123456789012@1ee36bc4-2a81-4917-8f1e-5a3c3728f94e.example.org"
        );
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}

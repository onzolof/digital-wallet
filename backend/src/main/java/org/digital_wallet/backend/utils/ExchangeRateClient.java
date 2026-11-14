package org.digital_wallet.backend.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExchangeRateClient {

    public static final String BASE_URL = "https://api.frankfurter.dev/v2";

    private final RestClient restClient;
    private final Map<String, BigDecimal> cache = new ConcurrentHashMap<>();

    @Autowired
    public ExchangeRateClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public BigDecimal fetchExchangeRate(String currency) {
        String normalizedCurrency = currency.toUpperCase();

        if ("CHF".equals(normalizedCurrency)) {
            return BigDecimal.ONE;
        }

        return cache.computeIfAbsent(normalizedCurrency, this::fetchFromApi);
    }

    private BigDecimal fetchFromApi(String currency) {
        ExchangeRateResponse response = restClient.get()
                .uri(BASE_URL + "/rate/{base}/CHF", currency)
                .retrieve()
                .body(ExchangeRateResponse.class);

        return response.rate();
    }

    private record ExchangeRateResponse(BigDecimal rate) {}
}

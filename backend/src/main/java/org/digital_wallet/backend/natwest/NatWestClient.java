package org.digital_wallet.backend.natwest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
public class NatWestClient {

    private static final Logger log = LoggerFactory.getLogger(NatWestClient.class);

    private static final String ACCESS_TOKEN_MAP_KEY = "natWestAccessToken";
    private static final String CONSENT_BODY = """
            {"Data":{"Permissions":["ReadAccountsDetail","ReadBalances","ReadTransactionsCredits","ReadTransactionsDebits","ReadTransactionsDetail","ReadProducts","ReadBeneficiariesDetail","ReadDirectDebits","ReadOffers","ReadScheduledPaymentsDetail","ReadStandingOrdersDetail","ReadStatementsDetail"]},"Risk":{}}""";
    public static final String ACCOUNTS_URL = "/open-banking/v4.0/aisp/accounts";
    public static final String TRANSACTIONS_URL = "/open-banking/v4.0/aisp/accounts/{accountId}/transactions";
    public static final int MAX_REDIRECTS = 5;
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final NatwestProperties properties;
    private final HashMap<String, String> tokens = new HashMap<>();

    @Autowired
    public NatWestClient(RestClient restClient, NatwestProperties properties, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public List<NatWestAccount> fetchAccounts() {
        return withAccessToken(this::readAccounts, true);
    }

    public List<NatWestTransaction> fetchTransactions(String accountId) {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        return withAccessToken(token -> readTransactions(token, accountId), true);
    }

    private <T> T withAccessToken(Function<String, T> call, boolean firstAttempt) {
        String token = firstAttempt ? cachedToken() : null;
        if (token == null) {
            token = authenticate();
            storeToken(token);
        }
        try {
            return call.apply(token);
        } catch (RestClientException ex) {
            if (!firstAttempt) {
                throw ex;
            }
            log.debug("NatWest repeating the consent flow");
            return withAccessToken(call, false);
        }
    }

    private String cachedToken() {
        // todo: test synchronization
        synchronized (tokens) {
            return tokens.get(ACCESS_TOKEN_MAP_KEY);
        }
    }

    private void storeToken(String token) {
        // todo: test synchronization
        synchronized (tokens) {
            tokens.put(ACCESS_TOKEN_MAP_KEY, token);
        }
    }

    private String authenticate() {
        Endpoints endpoints = discoverEndpoints();
        String clientToken = clientCredentialsToken(endpoints.tokenEndpoint());
        String consentId = createAccountAccessConsent(clientToken);
        String code = approveConsent(endpoints.authorizationEndpoint(), consentId);
        return exchangeAuthorizationCode(endpoints.tokenEndpoint(), code);
    }

    private Endpoints discoverEndpoints() {
        JsonNode response = get(properties.wellKnownEndpoint(), null);
        return new Endpoints(
                parseRequiredText(response, "authorization_endpoint"),
                parseRequiredText(response, "token_endpoint")
        );
    }

    private String clientCredentialsToken(String tokenEndpoint) {
        JsonNode response = postForm(tokenEndpoint, buildForm(
                "grant_type", "client_credentials",
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret(),
                "scope", "accounts"));
        return parseRequiredText(response, "access_token");
    }

    private String createAccountAccessConsent(String clientToken) {
        JsonNode response = postJson(
                properties.apiUrlPrefix() + "/open-banking/v4.0/aisp/account-access-consents",
                CONSENT_BODY,
                clientToken);
        JsonNode consentId = response.path("Data").path("ConsentId");
        if (consentId.isMissingNode() || consentId.asString().isBlank()) {
            throw new NatwestClientException("NatWest consent response did not include Data.ConsentId");
        }
        return consentId.asString();
    }

    private String approveConsent(String authorizationEndpoint, String consentId) {
        URI uri = UriComponentsBuilder.fromUriString(authorizationEndpoint)
                .queryParam("client_id", properties.clientId())
                .queryParam("response_type", "code id_token")
                .queryParam("scope", "openid accounts")
                .queryParam("redirect_uri", properties.redirectUrl())
                .queryParam("state", UUID.randomUUID().toString())
                .queryParam("request", consentId)
                .queryParam("authorization_mode", "AUTO_POSTMAN")
                .queryParam("authorization_username", properties.psuUsername())
                .build()
                .encode()
                .toUri();
        String body = getFollowRedirects(uri);
        return extractAuthorizationCode(parseRequiredText(readTree(body), "redirectUri"));
    }

    private String getFollowRedirects(URI uri) {
        for (int redirect = 0; redirect < MAX_REDIRECTS; redirect++) {
            URI current = uri;
            AuthorizationResponse step = restClient.get()
                    .uri(current)
                    .exchange((_, response) -> {
                        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
                        byte[] raw = response.getBody().readAllBytes();
                        return new AuthorizationResponse(response.getStatusCode().is3xxRedirection(), location,
                                new String(raw, StandardCharsets.UTF_8));
                    });
            if (!step.redirect()) {
                if (isBlank(step.body())) {
                    throw new NatwestClientException("NatWest authorization returned an empty response");
                }
                return step.body();
            }
            if (isBlank(step.location())) {
                throw new NatwestClientException("NatWest authorization redirect had no Location header");
            }
            uri = current.resolve(step.location());
        }
        throw new NatwestClientException("NatWest authorization redirected too many times");
    }

    private String exchangeAuthorizationCode(String tokenEndpoint, String code) {
        JsonNode response = postForm(tokenEndpoint, buildForm(
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret(),
                "redirect_uri", properties.redirectUrl(),
                "grant_type", "authorization_code",
                "code", code));
        return parseRequiredText(response, "access_token");
    }

    private List<NatWestAccount> readAccounts(String accessToken) {
        JsonNode accounts = get(properties.apiUrlPrefix() + ACCOUNTS_URL, accessToken)
                .path("Data")
                .path("Account");
        if (!accounts.isArray()) {
            return List.of();
        }
        List<NatWestAccount> result = new ArrayList<>();
        for (JsonNode account : accounts) {
            JsonNode identification = extractFirstIdentification(account);
            result.add(new NatWestAccount(
                    parseText(account, "AccountId"),
                    parseText(account, "Status"),
                    parseText(account, "Currency"),
                    parseText(account, "AccountCategory"),
                    parseText(account, "AccountTypeCode"),
                    parseText(account, "Nickname"),
                    parseText(identification, "SchemeName"),
                    parseText(identification, "Identification")));
        }
        return List.copyOf(result);
    }

    private List<NatWestTransaction> readTransactions(String accessToken, String accountId) {
        URI uri = UriComponentsBuilder.fromUriString(properties.apiUrlPrefix())
                .path(TRANSACTIONS_URL)
                .buildAndExpand(accountId)
                .encode()
                .toUri();
        JsonNode transactions = get(uri, accessToken).path("Data").path("Transaction");
        if (!transactions.isArray()) {
            return List.of();
        }
        List<NatWestTransaction> result = new ArrayList<>();
        for (JsonNode transaction : transactions) {
            JsonNode amount = transaction.path("Amount");
            result.add(new NatWestTransaction(
                    parseText(transaction, "AccountId"),
                    parseText(transaction, "TransactionId"),
                    parseText(amount, "Amount"),
                    parseText(amount, "Currency"),
                    parseText(transaction, "CreditDebitIndicator"),
                    parseText(transaction, "Status"),
                    parseText(transaction, "BookingDateTime"),
                    parseText(transaction, "TransactionInformation")));
        }
        return List.copyOf(result);
    }

    private JsonNode extractFirstIdentification(JsonNode account) {
        JsonNode identifications = account.path("Account");
        if (identifications.isArray() && !identifications.isEmpty()) {
            return identifications.get(0);
        }
        return identifications;
    }

    private JsonNode get(String url, String accessToken) {
        return get(URI.create(url), accessToken);
    }

    private JsonNode get(URI uri, String accessToken) {
        String body = restClient.get()
                .uri(uri)
                .headers(headers -> {
                    if (accessToken != null) {
                        headers.setBearerAuth(accessToken);
                    }
                })
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    private JsonNode postForm(String url, MultiValueMap<String, String> form) {
        String body = restClient.post()
                .uri(URI.create(url))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    private JsonNode postJson(String url, String json, String accessToken) {
        String body = restClient.post()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(String.class);
        return readTree(body);
    }

    private JsonNode readTree(String body) {
        if (isBlank(body)) {
            throw new NatwestClientException("NatWest returned an empty response");
        }
        return jsonMapper.readTree(body);
    }

    private static String extractAuthorizationCode(String redirectUri) {
        int hash = redirectUri.lastIndexOf('#');
        if (hash < 0 || hash == redirectUri.length() - 1) {
            throw new NatwestClientException("NatWest authorization response did not include a redirect fragment");
        }
        String code = null;
        String error = null;
        for (String pair : redirectUri.substring(hash + 1).split("&")) {
            int separator = pair.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            if ("code".equals(key)) {
                code = value;
            } else if ("error".equals(key)) {
                error = value;
            }
        }
        if (!isBlank(error)) {
            throw new NatwestClientException("NatWest authorization was rejected: " + error);
        }
        if (isBlank(code)) {
            throw new NatwestClientException("NatWest authorization response did not include a code");
        }
        return code;
    }

    private static String parseRequiredText(JsonNode node, String field) {
        String value = parseText(node, field);
        if (isBlank(value)) {
            throw new NatwestClientException("NatWest response is missing " + field);
        }
        return value;
    }

    private static String parseText(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = value.asString();
        return text.isBlank() ? null : text;
    }

    private static MultiValueMap<String, String> buildForm(String... pairs) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            form.add(pairs[i], pairs[i + 1]);
        }
        return form;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Endpoints(String authorizationEndpoint, String tokenEndpoint) {
    }

    private record AuthorizationResponse(boolean redirect, String location, String body) {
    }

}

package org.digital_wallet.backend.natwest;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

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

/**
 * Sandbox client for the NatWest CMA9 Account Information API.
 * The data-access token is stored in {@link #tokens}. A fetch uses that token
 * when one is present, and repeats the full consent flow once if the call fails.
 */
@Service
public class NatWestClient {

	private static final Logger log = LoggerFactory.getLogger(NatWestClient.class);

	private static final String ACCESS_TOKEN = "accessToken";

	private static final String CONSENT_BODY = """
			{"Data":{"Permissions":["ReadAccountsDetail","ReadBalances","ReadTransactionsCredits","ReadTransactionsDebits","ReadTransactionsDetail","ReadProducts","ReadBeneficiariesDetail","ReadDirectDebits","ReadOffers","ReadScheduledPaymentsDetail","ReadStandingOrdersDetail","ReadStatementsDetail"]},"Risk":{}}""";

	private final RestClient restClient;

	private final JsonMapper jsonMapper;

	private final NatwestProperties properties;

	private final HashMap<String, String> tokens = new HashMap<>();

	@Autowired
	public NatWestClient(NatwestProperties properties, JsonMapper jsonMapper) {
		this(RestClient.builder(), jsonMapper, properties);
	}

	NatWestClient(RestClient.Builder restClientBuilder, JsonMapper jsonMapper, NatwestProperties properties) {
		this.restClient = restClientBuilder.build();
		this.jsonMapper = jsonMapper;
		this.properties = properties;
	}

	public List<NatwestAccount> fetchAccounts() {
		return withAccessToken(this::readAccounts);
	}

	public List<NatwestTransaction> fetchTransactions(String accountId) {
		if (accountId == null || accountId.isBlank()) {
			throw new IllegalArgumentException("accountId is required");
		}
		return withAccessToken(token -> readTransactions(token, accountId));
	}

	private <T> T withAccessToken(Function<String, T> call) {
		String token = cachedToken();
		if (token == null) {
			token = authenticate();
			storeToken(token);
		}
		try {
			return call.apply(token);
		}
		catch (RestClientException ex) {
			log.warn("NatWest call failed; repeating the consent flow");
			clearToken();
			String refreshed = authenticate();
			storeToken(refreshed);
			return call.apply(refreshed);
		}
	}

	private String cachedToken() {
		synchronized (tokens) {
			return tokens.get(ACCESS_TOKEN);
		}
	}

	private void storeToken(String token) {
		synchronized (tokens) {
			tokens.put(ACCESS_TOKEN, token);
		}
	}

	private void clearToken() {
		synchronized (tokens) {
			tokens.remove(ACCESS_TOKEN);
		}
	}

	private String authenticate() {
		requireConfigured();
		Endpoints endpoints = discoverEndpoints();
		String clientToken = clientCredentialsToken(endpoints.tokenEndpoint());
		String consentId = createAccountAccessConsent(clientToken);
		String code = approveConsent(endpoints.authorizationEndpoint(), consentId);
		return exchangeAuthorizationCode(endpoints.tokenEndpoint(), code);
	}

	private Endpoints discoverEndpoints() {
		JsonNode document = get(properties.wellKnownEndpoint(), null);
		return new Endpoints(
				requiredText(document, "authorization_endpoint"),
				requiredText(document, "token_endpoint"));
	}

	private String clientCredentialsToken(String tokenEndpoint) {
		JsonNode response = postForm(tokenEndpoint, form(
				"grant_type", "client_credentials",
				"client_id", properties.clientId(),
				"client_secret", properties.clientSecret(),
				"scope", "accounts"));
		return requiredText(response, "access_token");
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
		URI authorizationUri = UriComponentsBuilder.fromUriString(authorizationEndpoint)
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
		JsonNode response = get(authorizationUri, null);
		return authorizationCode(requiredText(response, "redirectUri"));
	}

	private String exchangeAuthorizationCode(String tokenEndpoint, String code) {
		JsonNode response = postForm(tokenEndpoint, form(
				"client_id", properties.clientId(),
				"client_secret", properties.clientSecret(),
				"redirect_uri", properties.redirectUrl(),
				"grant_type", "authorization_code",
				"code", code));
		return requiredText(response, "access_token");
	}

	private List<NatwestAccount> readAccounts(String accessToken) {
		JsonNode accounts = get(properties.apiUrlPrefix() + "/open-banking/v4.0/aisp/accounts", accessToken)
				.path("Data")
				.path("Account");
		if (!accounts.isArray()) {
			return List.of();
		}
		List<NatwestAccount> result = new ArrayList<>();
		for (JsonNode account : accounts) {
			JsonNode identification = firstIdentification(account);
			result.add(new NatwestAccount(
					text(account, "AccountId"),
					text(account, "Status"),
					text(account, "Currency"),
					text(account, "AccountType"),
					text(account, "AccountSubType"),
					text(account, "Nickname"),
					text(identification, "SchemeName"),
					text(identification, "Identification")));
		}
		return List.copyOf(result);
	}

	private List<NatwestTransaction> readTransactions(String accessToken, String accountId) {
		URI uri = UriComponentsBuilder.fromUriString(properties.apiUrlPrefix())
				.path("/open-banking/v4.0/aisp/accounts/{accountId}/transactions")
				.buildAndExpand(accountId)
				.encode()
				.toUri();
		JsonNode transactions = get(uri, accessToken).path("Data").path("Transaction");
		if (!transactions.isArray()) {
			return List.of();
		}
		List<NatwestTransaction> result = new ArrayList<>();
		for (JsonNode transaction : transactions) {
			JsonNode amount = transaction.path("Amount");
			result.add(new NatwestTransaction(
					text(transaction, "AccountId"),
					text(transaction, "TransactionId"),
					text(amount, "Amount"),
					text(amount, "Currency"),
					text(transaction, "CreditDebitIndicator"),
					text(transaction, "Status"),
					text(transaction, "BookingDateTime"),
					text(transaction, "TransactionInformation")));
		}
		return List.copyOf(result);
	}

	private JsonNode firstIdentification(JsonNode account) {
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
		if (body == null || body.isBlank()) {
			throw new NatwestClientException("NatWest returned an empty response");
		}
		return jsonMapper.readTree(body);
	}

	private void requireConfigured() {
		if (isBlank(properties.wellKnownEndpoint())
				|| isBlank(properties.apiUrlPrefix())
				|| isBlank(properties.clientId())
				|| isBlank(properties.clientSecret())
				|| isBlank(properties.redirectUrl())
				|| isBlank(properties.psuUsername())) {
			throw new NatwestClientException(
					"NatWest client settings are incomplete. Set natwest.well-known-endpoint, api-url-prefix, client-id, client-secret, redirect-url and psu-username.");
		}
	}

	private static String authorizationCode(String redirectUri) {
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
			}
			else if ("error".equals(key)) {
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

	private static String requiredText(JsonNode node, String field) {
		String value = text(node, field);
		if (isBlank(value)) {
			throw new NatwestClientException("NatWest response is missing " + field);
		}
		return value;
	}

	private static String text(JsonNode node, String field) {
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

	private static MultiValueMap<String, String> form(String... pairs) {
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

}

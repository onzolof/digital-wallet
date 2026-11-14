package org.digital_wallet.backend.natwest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

class NatWestClientTest {

    private static final String WELL_KNOWN = "https://api.example/.well-known/openid-configuration";
    private static final String TOKEN = "https://api.example/token";
    private static final String AUTHORIZE = "https://api.example/authorize";
    private static final String BASE_URL = "https://ob.example";
    private static final String ACCOUNTS = BASE_URL + "/open-banking/v4.0/aisp/accounts";
    private static final String CONSENT = BASE_URL + "/open-banking/v4.0/aisp/account-access-consents";
    private static final NatwestProperties NATWEST_PROPERTIES = new NatwestProperties(
            WELL_KNOWN,
            BASE_URL,
            "client-id",
            "client-secret",
            "http://localhost/callback",
            "123456789012@example.org");

    private MockRestServiceServer server;
    private NatWestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        client = new NatWestClient(restClientBuilder.build(), NATWEST_PROPERTIES, JsonMapper.builder().build());
    }

    @Test
    void fetchAccountsRunsTheConsentFlowAndCachesTheToken() {
        expectConsentFlow("api-token");
        server.expect(requestTo(ACCOUNTS))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer api-token"))
                .andRespond(withSuccess(accountsJson(), APPLICATION_JSON));

        assertThat(client.fetchAccounts()).singleElement().satisfies(account -> {
            assertThat(account.accountId()).isEqualTo("acc-1");
            assertThat(account.currency()).isEqualTo("GBP");
            assertThat(account.nickname()).isEqualTo("Household");
            assertThat(account.schemeName()).isEqualTo("UK.OBIE.SortCodeAccountNumber");
            assertThat(account.identification()).isEqualTo("50000012345601");
        });
        server.verify();

        server.reset();
        server.expect(requestTo(ACCOUNTS))
                .andExpect(header(AUTHORIZATION, "Bearer api-token"))
                .andRespond(withSuccess(accountsJson(), APPLICATION_JSON));

        assertThat(client.fetchAccounts()).singleElement().satisfies(account ->
                assertThat(account.accountId()).isEqualTo("acc-1"));
        server.verify();
    }

    @Test
    void fetchAccountsFollowsTheAuthorizationRedirect() {
        server.expect(requestTo(WELL_KNOWN))
                .andRespond(withSuccess("""
                        {"authorization_endpoint":"%s","token_endpoint":"%s"}
                        """.formatted(AUTHORIZE, TOKEN), APPLICATION_JSON));
        server.expect(requestTo(TOKEN))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andRespond(withSuccess("{\"access_token\":\"client-token\"}", APPLICATION_JSON));
        server.expect(requestTo(CONSENT))
                .andRespond(withSuccess("{\"Data\":{\"ConsentId\":\"consent-1\"}}", APPLICATION_JSON));
        server.expect(requestTo(containsString("authorization_mode=AUTO_POSTMAN")))
                .andRespond(withStatus(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, BASE_URL + "/consent/authorize"));
        server.expect(requestTo(BASE_URL + "/consent/authorize"))
                .andRespond(withSuccess(
                        "{\"redirectUri\":\"http://localhost/callback#code=auth-code&state=ABC\"}",
                        APPLICATION_JSON));
        server.expect(requestTo(TOKEN))
                .andExpect(content().string(containsString("code=auth-code")))
                .andRespond(withSuccess("{\"access_token\":\"api-token\"}", APPLICATION_JSON));
        server.expect(requestTo(ACCOUNTS))
                .andExpect(header(AUTHORIZATION, "Bearer api-token"))
                .andRespond(withSuccess(accountsJson(), APPLICATION_JSON));

        assertThat(client.fetchAccounts()).singleElement().satisfies(account ->
                assertThat(account.accountId()).isEqualTo("acc-1"));
        server.verify();
    }

    @Test
    void fetchRepeatsTheConsentFlowWhenTheCachedTokenIsRejected() {
        expectConsentFlow("api-token");
        server.expect(requestTo(ACCOUNTS))
                .andRespond(withSuccess(accountsJson(), APPLICATION_JSON));
        client.fetchAccounts();
        server.verify();

        server.reset();
        server.expect(requestTo(ACCOUNTS))
                .andExpect(header(AUTHORIZATION, "Bearer api-token"))
                .andRespond(withUnauthorizedRequest());
        expectConsentFlow("refreshed-token");
        server.expect(requestTo(ACCOUNTS))
                .andExpect(header(AUTHORIZATION, "Bearer refreshed-token"))
                .andRespond(withSuccess(accountsJson(), APPLICATION_JSON));

        assertThat(client.fetchAccounts()).hasSize(1);
        server.verify();
    }

    @Test
    void secondFailureAfterARepeatedConsentFlowPropagates() {
        expectConsentFlow("api-token");
        server.expect(requestTo(ACCOUNTS)).andRespond(withServerError());
        expectConsentFlow("refreshed-token");
        server.expect(requestTo(ACCOUNTS)).andRespond(withServerError());

        assertThatThrownBy(client::fetchAccounts).isInstanceOf(RestClientResponseException.class);
        server.verify();
    }

    @Test
    void fetchTransactionsUsesTheAccountPath() {
        expectConsentFlow("api-token");
        server.expect(requestTo(ACCOUNTS + "/acc-1/transactions"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AUTHORIZATION, "Bearer api-token"))
                .andRespond(withSuccess(transactionsJson(), APPLICATION_JSON));

        assertThat(client.fetchTransactions("acc-1")).singleElement().satisfies(transaction -> {
            assertThat(transaction.transactionId()).isEqualTo("tx-1");
            assertThat(transaction.amount()).isEqualTo("3.50");
            assertThat(transaction.currency()).isEqualTo("GBP");
            assertThat(transaction.creditDebitIndicator()).isEqualTo("Debit");
            assertThat(transaction.bookingDateTime()).isEqualTo("2025-01-30T06:40:00.000Z");
            assertThat(transaction.transactionInformation()).isEqualTo("Coffee");
        });
        server.verify();
    }

    @Test
    void fetchTransactionsRejectsABlankAccountId() {
        assertThatThrownBy(() -> client.fetchTransactions(" ")).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    private void expectConsentFlow(String apiToken) {
        server.expect(requestTo(WELL_KNOWN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"authorization_endpoint":"%s","token_endpoint":"%s"}
                        """.formatted(AUTHORIZE, TOKEN), APPLICATION_JSON));
        server.expect(requestTo(TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andExpect(content().string(containsString("scope=accounts")))
                .andRespond(withSuccess("{\"access_token\":\"client-token\"}", APPLICATION_JSON));
        server.expect(requestTo(CONSENT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AUTHORIZATION, "Bearer client-token"))
                .andExpect(content().string(containsString("ReadTransactionsDetail")))
                .andRespond(withSuccess("{\"Data\":{\"ConsentId\":\"consent-1\"}}", APPLICATION_JSON));
        server.expect(requestTo(containsString("authorization_mode=AUTO_POSTMAN")))
                .andExpect(requestTo(containsString("request=consent-1")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"redirectUri\":\"http://localhost/callback#code=auth-code&state=ABC\"}",
                        APPLICATION_JSON));
        server.expect(requestTo(TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("grant_type=authorization_code")))
                .andExpect(content().string(containsString("code=auth-code")))
                .andRespond(withSuccess("{\"access_token\":\"" + apiToken + "\"}", APPLICATION_JSON));
    }

    private static String accountsJson() {
        return """
                {"Data":{"Account":[{
                  "AccountId":"acc-1",
                  "Status":"Enabled",
                  "Currency":"GBP",
                  "AccountType":"Personal",
                  "AccountSubType":"CurrentAccount",
                  "Nickname":"Household",
                  "Account":[{"SchemeName":"UK.OBIE.SortCodeAccountNumber","Identification":"50000012345601"}]
                }]}}
                """;
    }

    private static String transactionsJson() {
        return """
                {"Data":{"Transaction":[{
                  "AccountId":"acc-1",
                  "TransactionId":"tx-1",
                  "CreditDebitIndicator":"Debit",
                  "Status":"Booked",
                  "BookingDateTime":"2025-01-30T06:40:00.000Z",
                  "TransactionInformation":"Coffee",
                  "Amount":{"Amount":"3.50","Currency":"GBP"}
                }]}}
                """;
    }

}

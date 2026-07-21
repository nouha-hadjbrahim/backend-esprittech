package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tn.esprit.backend_esprittech.exception.AzureGraphException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GraphTokenProviderTest {

    private static final String LOGIN_BASE = "https://login.example.tn";
    private static final String TOKEN_URI = LOGIN_BASE + "/tenant-esprit/oauth2/v2.0/token";

    private MockRestServiceServer server;
    private GraphTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(LOGIN_BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GraphTokenProvider(builder.build(), properties());
    }

    @Test
    void accessToken_shouldRequestTokenOnce_andReuseItWhileValid() {
        server.expect(org.springframework.test.web.client.ExpectedCount.once(), requestTo(TOKEN_URI))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(tokenJson("jeton-abc", 3600), MediaType.APPLICATION_JSON));

        assertEquals("jeton-abc", provider.accessToken());
        // Deuxieme appel : servi depuis le cache, aucune requete supplementaire attendue.
        assertEquals("jeton-abc", provider.accessToken());

        server.verify();
    }

    @Test
    void bearer_shouldPrefixTokenWithBearer() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenJson("jeton-abc", 3600), MediaType.APPLICATION_JSON));

        assertEquals("Bearer jeton-abc", provider.bearer());
    }

    @Test
    void accessToken_shortLivedToken_shouldBeRequestedAgain() {
        // expiresIn <= marge de securite (60s) => TTL nul, le cache est immediatement perime
        server.expect(org.springframework.test.web.client.ExpectedCount.twice(), requestTo(TOKEN_URI))
                .andRespond(withSuccess(tokenJson("jeton-court", 30), MediaType.APPLICATION_JSON));

        assertEquals("jeton-court", provider.accessToken());
        assertEquals("jeton-court", provider.accessToken());

        server.verify();
    }

    @Test
    void accessToken_emptyResponse_shouldReportAzureGraphError() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AzureGraphException ex = assertThrows(AzureGraphException.class, () -> provider.accessToken());

        assertEquals("Réponse de token Azure AD vide.", ex.getMessage());
    }

    @Test
    void accessToken_serverError_shouldReportAzureGraphErrorWithTenant() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withServerError());

        AzureGraphException ex = assertThrows(AzureGraphException.class, () -> provider.accessToken());

        assertTrue(ex.getMessage().contains("tenant-esprit"));
    }

    @Test
    void accessToken_unauthorized_shouldReportAzureGraphError() {
        server.expect(requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"invalid_client\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(AzureGraphException.class, () -> provider.accessToken());
    }

    private String tokenJson(String token, long expiresIn) {
        return "{\"access_token\":\"" + token + "\",\"expires_in\":" + expiresIn + "}";
    }

    private AzureGraphProperties properties() {
        return new AzureGraphProperties(
                "tenant-esprit", "client-id", "client-secret",
                "groupe-enseignant", "groupe-etudiant",
                LOGIN_BASE, "https://graph.example.tn", "noreply@esprit.tn");
    }
}

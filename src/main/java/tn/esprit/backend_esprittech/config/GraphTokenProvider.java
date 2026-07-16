package tn.esprit.backend_esprittech.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.backend_esprittech.exception.AzureGraphException;

import java.time.Instant;

/**
 * Fournit un jeton d'application Microsoft Graph (flux client credentials),
 * mis en cache jusqu'à peu avant son expiration. Partagé par tous les
 * appelants Graph (lecture des utilisateurs, envoi d'emails).
 */
@Component
public class GraphTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(GraphTokenProvider.class);
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    /** Marge de sécurité : on renouvelle le token un peu avant son expiration réelle. */
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 60;

    private final RestClient loginClient;
    private final AzureGraphProperties properties;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public GraphTokenProvider(@Qualifier("azureLoginRestClient") RestClient loginClient,
                              AzureGraphProperties properties) {
        this.loginClient = loginClient;
        this.properties = properties;
    }

    /** En-tête Authorization prêt à l'emploi ("Bearer &lt;token&gt;"). */
    public String bearer() {
        return "Bearer " + accessToken();
    }

    /** Jeton d'application mis en cache jusqu'à peu avant son expiration. */
    public String accessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        synchronized (this) {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            TokenResponse token = requestToken();
            cachedToken = token.accessToken();
            long ttl = Math.max(0, token.expiresIn() - TOKEN_EXPIRY_MARGIN_SECONDS);
            tokenExpiresAt = Instant.now().plusSeconds(ttl);
            return cachedToken;
        }
    }

    private TokenResponse requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("scope", GRAPH_SCOPE);
        form.add("grant_type", "client_credentials");
        try {
            TokenResponse token = loginClient.post()
                    .uri("/{tenant}/oauth2/v2.0/token", properties.tenantId())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.accessToken() == null) {
                throw new AzureGraphException("Réponse de token Azure AD vide.", null);
            }
            return token;
        } catch (RestClientException e) {
            log.error("Échec d'obtention du token Azure AD (tenant {})", properties.tenantId(), e);
            throw new AzureGraphException("Authentification à Azure AD impossible pour le moment.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}

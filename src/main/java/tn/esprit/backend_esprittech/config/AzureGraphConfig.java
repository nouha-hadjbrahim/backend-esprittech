package tn.esprit.backend_esprittech.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Beans {@link RestClient} pour dialoguer avec Azure AD (obtention du token)
 * et Microsoft Graph (lecture des utilisateurs et de leurs groupes).
 */
@Configuration
@EnableConfigurationProperties({AzureGraphProperties.class, VerificationProperties.class})
public class AzureGraphConfig {

    /** Client vers login.microsoftonline.com pour le flux client_credentials. */
    @Bean
    public RestClient azureLoginRestClient(AzureGraphProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.loginBaseUrl())
                .build();
    }

    /** Client vers l'API Microsoft Graph (v1.0). */
    @Bean
    public RestClient azureGraphRestClient(AzureGraphProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .build();
    }
}

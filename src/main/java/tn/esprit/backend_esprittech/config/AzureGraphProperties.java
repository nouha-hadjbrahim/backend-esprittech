package tn.esprit.backend_esprittech.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres d'accès à Azure Entra ID / Microsoft Graph (flux client credentials).
 * Utilisés pour vérifier, à l'inscription, l'appartenance d'un email aux groupes
 * enseignant / étudiant de l'organisation ESPRIT.
 */
@ConfigurationProperties(prefix = "azure.graph")
public record AzureGraphProperties(
        String tenantId,
        String clientId,
        String clientSecret,
        String enseignantGroupId,
        String etudiantGroupId,
        String loginBaseUrl,
        String apiBaseUrl,
        /** Boîte aux lettres ESPRIT utilisée comme expéditeur (From) des emails via Graph sendMail. */
        String mailSender
) {
}

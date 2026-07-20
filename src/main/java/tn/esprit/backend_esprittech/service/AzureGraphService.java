package tn.esprit.backend_esprittech.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.backend_esprittech.config.AzureGraphProperties;
import tn.esprit.backend_esprittech.config.GraphTokenProvider;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.AzureGraphException;
import tn.esprit.backend_esprittech.exception.OrganizationMembershipException;

import java.util.List;
import java.util.Map;

/**
 * Vérifie, à l'inscription, qu'un email appartient à l'organisation ESPRIT et
 * détermine son rôle (enseignant / étudiant) via l'appartenance aux groupes
 * Azure Entra ID, en remplacement de l'ancien référentiel CSV.
 *
 * <p>Flux : recherche de l'utilisateur par email dans Microsoft Graph &rarr;
 * {@code checkMemberGroups} pour tester l'appartenance aux deux groupes cibles.</p>
 *
 * <p>Règle de rôle : étudiant uniquement si membre du groupe étudiant et pas du
 * groupe enseignant ; sinon enseignant (défaut, y compris si aucun groupe ou les
 * deux).</p>
 */
@Service
public class AzureGraphService {

    private static final Logger log = LoggerFactory.getLogger(AzureGraphService.class);

    private final RestClient graphClient;
    private final GraphTokenProvider tokenProvider;
    private final AzureGraphProperties properties;

    public AzureGraphService(@Qualifier("azureGraphRestClient") RestClient graphClient,
                             GraphTokenProvider tokenProvider,
                             AzureGraphProperties properties) {
        this.graphClient = graphClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    /**
     * Résout un email vers un membre de l'organisation (rôle + département).
     *
     * @throws OrganizationMembershipException si l'email n'appartient à aucun
     *         utilisateur de l'organisation.
     * @throws AzureGraphException en cas d'échec technique d'appel à Azure/Graph.
     */
    public GraphMember resolveMember(String email) {
        GraphUser user = findUserByEmail(email);
        if (user == null) {
            throw new OrganizationMembershipException(
                    "Cet email n'appartient pas à l'organisation ESPRIT. Impossible de créer un compte.");
        }
        TypeUtilisateur type = resolveType(user.id());
        log.info("Résolution Azure : email={} type={} departement={}", email, type, user.department());
        return new GraphMember(type, user.department());
    }

    /** Détermine le type d'utilisateur à partir de l'appartenance aux deux groupes. */
    private TypeUtilisateur resolveType(String userId) {
        List<String> groups = checkMemberGroups(userId);
        boolean enseignant = groups.contains(properties.enseignantGroupId());
        boolean etudiant = groups.contains(properties.etudiantGroupId());
        // Étudiant uniquement si dans le groupe étudiant et PAS dans le groupe enseignant.
        // Sinon enseignant : défaut quand aucun groupe, et priorité enseignant si les deux.
        return (etudiant && !enseignant) ? TypeUtilisateur.ETUDIANT : TypeUtilisateur.ENSEIGNANT;
    }

    /** Recherche un utilisateur par email (mail ou userPrincipalName), ou {@code null}. */
    private GraphUser findUserByEmail(String email) {
        String escaped = email.replace("'", "''");
        String filter = "mail eq '" + escaped + "' or userPrincipalName eq '" + escaped + "'";
        try {
            GraphCollection<GraphUser> response = graphClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/users")
                            .queryParam("$filter", filter)
                            .queryParam("$select", "id,department")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.value() == null || response.value().isEmpty()) {
                return null;
            }
            return response.value().get(0);
        } catch (RestClientException e) {
            throw new AzureGraphException(
                    "Vérification de l'organisation indisponible pour le moment (utilisateur " + email + ").", e);
        }
    }

    /** Renvoie, parmi les deux groupes cibles, ceux dont l'utilisateur est membre (transitif). */
    private List<String> checkMemberGroups(String userId) {
        Map<String, Object> body = Map.of(
                "groupIds", List.of(properties.enseignantGroupId(), properties.etudiantGroupId()));
        try {
            GraphStringCollection response = graphClient.post()
                    .uri("/users/{id}/checkMemberGroups", userId)
                    .header(HttpHeaders.AUTHORIZATION, tokenProvider.bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GraphStringCollection.class);
            return (response == null || response.value() == null) ? List.of() : response.value();
        } catch (RestClientException e) {
            throw new AzureGraphException(
                    "Vérification des groupes indisponible pour le moment (utilisateur " + userId + ").", e);
        }
    }

    // ----- Résultat exposé -----

    /** Membre résolu depuis Azure : rôle métier + département (peut être {@code null}). */
    public record GraphMember(TypeUtilisateur type, String departement) {
    }

    // ----- Représentations JSON internes (Graph) -----

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphUser(String id, String department) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphCollection<T>(List<T> value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphStringCollection(List<String> value) {
    }
}

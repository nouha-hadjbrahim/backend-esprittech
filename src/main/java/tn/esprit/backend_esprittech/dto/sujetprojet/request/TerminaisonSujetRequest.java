package tn.esprit.backend_esprittech.dto.sujetprojet.request;

/**
 * Données optionnelles lors de la terminaison d'un sujet.
 * L'image de couverture est publiée avec le projet catalogue créé automatiquement.
 */
public record TerminaisonSujetRequest(
        /** Image de couverture encodée en base64 (data URI ou base64 brut), optionnelle. */
        String coverImageBase64,
        /** Type MIME de l'image (image/png ou image/jpeg). */
        String coverImageContentType
) {
}

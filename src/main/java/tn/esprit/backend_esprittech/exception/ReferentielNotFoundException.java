package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'aucune correspondance n'est trouvée dans le référentiel
 * (ni par email, ni par identifiant) lors d'une inscription.
 */
public class ReferentielNotFoundException extends RuntimeException {
    public ReferentielNotFoundException(String message) {
        super(message);
    }
}

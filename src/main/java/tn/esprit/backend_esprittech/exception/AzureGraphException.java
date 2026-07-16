package tn.esprit.backend_esprittech.exception;

/**
 * Levée lorsqu'un appel à Azure AD / Microsoft Graph échoue pour une raison
 * technique (token indisponible, appel Graph en erreur). Distincte de
 * {@link OrganizationMembershipException} qui traduit une absence métier.
 */
public class AzureGraphException extends RuntimeException {
    public AzureGraphException(String message, Throwable cause) {
        super(message, cause);
    }
}

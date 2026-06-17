package tn.esprit.backend_esprittech.enums;

/**
 * Rôles applicatifs. Le préfixe ROLE_ est requis par Spring Security
 * pour que hasRole(...) et @PreAuthorize("hasRole('...')") fonctionnent.
 */
public enum Role {
    ROLE_ETUDIANT,
    ROLE_ENSEIGNANT,
    ROLE_CHEF_EQUIPE,
    ROLE_ADMIN,
    ROLE_CI
}

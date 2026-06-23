package tn.esprit.backend_esprittech.service.sujetprojet;

/**
 * Encadrant statique utilisé pour les tests sans dépendre de l'entité User.
 */
public final class MockEncadrantContext {

    public static final Long ENCADRANT_ID = 1001L;
    public static final String NOM_COMPLET = "Dr. Ben Ali Sami";
    public static final String ROLE = "Encadrant";
    public static final String EMAIL = "benali.sami@esprittech.tn";
    public static final String DEPARTEMENT = "Informatique";

    private MockEncadrantContext() {
    }
}

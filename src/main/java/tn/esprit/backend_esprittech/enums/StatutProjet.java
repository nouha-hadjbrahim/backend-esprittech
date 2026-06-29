package tn.esprit.backend_esprittech.enums;

import java.util.List;

/**
 * Cycle de vie d'un projet du catalogue applicatif.
 *
 * <p>Un projet est créé en {@link #SOUMIS_EN_VALIDATION}. Le chef de l'équipe de
 * recherche le {@link #VALIDE} (il devient alors visible dans le catalogue public)
 * ou le passe en {@link #INVALIDE} (refus motivé, jamais publié). Les statuts
 * {@code CANDIDAT_INDUSTRIALISATION_*} et {@code INDUSTRIALISE_*} correspondent aux
 * étapes ultérieures d'industrialisation, toutes visibles dans le catalogue.</p>
 */
public enum StatutProjet {
    SOUMIS_EN_VALIDATION,
    INVALIDE,
    VALIDE,
    CANDIDAT_INDUSTRIALISATION_INTERNE,
    CANDIDAT_INDUSTRIALISATION_EXTERNE,
    INDUSTRIALISE_DSI,
    INDUSTRIALISE_EXTERNE;

    /**
     * Statuts pour lesquels un projet est visible dans le catalogue public.
     * {@link #SOUMIS_EN_VALIDATION} et {@link #INVALIDE} en sont volontairement exclus.
     */
    public static final List<StatutProjet> STATUTS_CATALOGUE = List.of(
            VALIDE,
            CANDIDAT_INDUSTRIALISATION_INTERNE,
            CANDIDAT_INDUSTRIALISATION_EXTERNE,
            INDUSTRIALISE_DSI,
            INDUSTRIALISE_EXTERNE
    );
}

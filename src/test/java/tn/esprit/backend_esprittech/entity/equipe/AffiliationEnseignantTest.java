package tn.esprit.backend_esprittech.entity.equipe;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AffiliationEnseignantTest {

    @Test
    void onCreate_dateDemandeNull_setsValue() {
        AffiliationEnseignant affiliation = new AffiliationEnseignant();
        affiliation.setDateDemande(null);

        affiliation.onCreate();

        assertNotNull(affiliation.getDateDemande());
    }

    @Test
    void onCreate_dateDemandeAlreadySet_keepsValue() {
        AffiliationEnseignant affiliation = new AffiliationEnseignant();
        LocalDateTime fixed = LocalDateTime.of(2024, 6, 15, 10, 0);
        affiliation.setDateDemande(fixed);

        affiliation.onCreate();

        assertEquals(fixed, affiliation.getDateDemande());
    }

    @Test
    void builder_setsFields() {
        User enseignant = new User();
        enseignant.setId(1L);
        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setId(10L);

        AffiliationEnseignant affiliation = AffiliationEnseignant.builder()
                .id(1L)
                .enseignant(enseignant)
                .equipe(equipe)
                .statut(StatutAffiliation.EN_ATTENTE)
                .motifDecision("Motif")
                .build();

        assertEquals(1L, affiliation.getId());
        assertEquals(enseignant, affiliation.getEnseignant());
        assertEquals(equipe, affiliation.getEquipe());
        assertEquals(StatutAffiliation.EN_ATTENTE, affiliation.getStatut());
        assertEquals("Motif", affiliation.getMotifDecision());
    }

    @Test
    void builder_defaultStatut_isEnAttente() {
        AffiliationEnseignant affiliation = AffiliationEnseignant.builder().build();

        assertEquals(StatutAffiliation.EN_ATTENTE, affiliation.getStatut());
    }

    @Test
    void allArgsConstructor_setsFields() {
        User enseignant = new User();
        enseignant.setId(1L);
        EquipeRecherche equipe = new EquipeRecherche();
        equipe.setId(10L);
        LocalDateTime dateDemande = LocalDateTime.now();
        LocalDateTime dateDecision = LocalDateTime.now().plusDays(1);

        AffiliationEnseignant affiliation = new AffiliationEnseignant(
                1L, enseignant, equipe, StatutAffiliation.ACCEPTEE,
                dateDemande, dateDecision, "Accepté"
        );

        assertEquals(1L, affiliation.getId());
        assertEquals(enseignant, affiliation.getEnseignant());
        assertEquals(equipe, affiliation.getEquipe());
        assertEquals(StatutAffiliation.ACCEPTEE, affiliation.getStatut());
        assertEquals(dateDemande, affiliation.getDateDemande());
        assertEquals(dateDecision, affiliation.getDateDecision());
        assertEquals("Accepté", affiliation.getMotifDecision());
    }
}

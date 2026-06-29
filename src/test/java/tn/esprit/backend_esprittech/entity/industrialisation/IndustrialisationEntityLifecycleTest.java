package tn.esprit.backend_esprittech.entity.industrialisation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndustrialisationEntityLifecycleTest {

    @Test
    void questionOnCreate_shouldSetDatesAndDefaultFlagsWhenMissing() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setActif(null);
        question.setObligatoire(null);

        question.onCreate();

        assertNotNull(question.getDateCreation());
        assertNotNull(question.getDateMiseAJour());
        assertTrue(question.getActif());
        assertTrue(question.getObligatoire());
    }

    @Test
    void questionOnUpdate_shouldRefreshUpdateDate() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();

        question.onUpdate();

        assertNotNull(question.getDateMiseAJour());
    }

    @Test
    void candidatureOnCreate_shouldSetMissingDateAndStatusAndPreserveExistingValues() {
        CandidatureIndustrialisation missingDefaults = new CandidatureIndustrialisation();
        missingDefaults.setStatut(null);

        missingDefaults.onCreate();

        assertNotNull(missingDefaults.getDateDemande());
        assertEquals(StatutIndustrialisation.BROUILLON, missingDefaults.getStatut());

        CandidatureIndustrialisation existingValues = new CandidatureIndustrialisation();
        LocalDateTime existingDate = LocalDateTime.of(2026, 1, 1, 9, 0);
        existingValues.setDateDemande(existingDate);
        existingValues.setStatut(StatutIndustrialisation.RECUE_PAR_CI);

        existingValues.onCreate();

        assertSame(existingDate, existingValues.getDateDemande());
        assertEquals(StatutIndustrialisation.RECUE_PAR_CI, existingValues.getStatut());
    }

    @Test
    void reponseLifecycle_shouldSetAndRefreshResponseDate() {
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();

        reponse.onCreate();
        LocalDateTime created = reponse.getDateReponse();
        reponse.onUpdate();

        assertNotNull(created);
        assertNotNull(reponse.getDateReponse());
    }

    @Test
    void historiqueOnCreate_shouldSetMissingDateAndPreserveExistingDate() {
        HistoriqueIndustrialisation missingDate = new HistoriqueIndustrialisation();
        missingDate.onCreate();
        assertNotNull(missingDate.getDateTransition());

        HistoriqueIndustrialisation existingDate = new HistoriqueIndustrialisation();
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 9, 0);
        existingDate.setDateTransition(fixed);
        existingDate.onCreate();
        assertSame(fixed, existingDate.getDateTransition());
    }
}

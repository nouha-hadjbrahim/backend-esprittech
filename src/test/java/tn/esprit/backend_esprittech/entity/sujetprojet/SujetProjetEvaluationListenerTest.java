package tn.esprit.backend_esprittech.entity.sujetprojet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.backend_esprittech.config.ApplicationContextProvider;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.critereeval.EvaluationService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SujetProjetEvaluationListenerTest {

    private final SujetProjetEvaluationListener listener = new SujetProjetEvaluationListener();
    private final EvaluationService evaluationService = mock(EvaluationService.class);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void onPostUpdate_terminatedProjectWithoutScore_shouldTriggerEvaluation() {
        bindApplicationContext();

        listener.onPostUpdate(sujet(StatutSujet.REALISATION_TERMINEE, null));

        verify(evaluationService).calculateEvaluationForSystem(42L);
    }

    @Test
    void onPostUpdate_projectNotTerminated_shouldDoNothing() {
        listener.onPostUpdate(sujet(StatutSujet.REALISATION_EN_COURS, null));

        verify(evaluationService, never()).calculateEvaluationForSystem(anyId());
    }

    @Test
    void onPostUpdate_alreadyScoredProject_shouldDoNothing() {
        listener.onPostUpdate(sujet(StatutSujet.REALISATION_TERMINEE, 82.0));

        verify(evaluationService, never()).calculateEvaluationForSystem(anyId());
    }

    @Test
    void onPostUpdate_withinTransaction_shouldDeferUntilAfterCommit() {
        bindApplicationContext();
        TransactionSynchronizationManager.initSynchronization();

        listener.onPostUpdate(sujet(StatutSujet.REALISATION_TERMINEE, null));

        // Tant que la transaction n'est pas validee, aucune evaluation ne doit partir.
        verify(evaluationService, never()).calculateEvaluationForSystem(42L);

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(evaluationService).calculateEvaluationForSystem(42L);
    }

    @Test
    void onPostUpdate_whenEvaluationFails_shouldNotPropagateException() {
        bindApplicationContext();
        when(evaluationService.calculateEvaluationForSystem(42L))
                .thenThrow(new IllegalStateException("moteur indisponible"));

        assertDoesNotThrow(() -> listener.onPostUpdate(sujet(StatutSujet.REALISATION_TERMINEE, null)));
    }

    private void bindApplicationContext() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(EvaluationService.class)).thenReturn(evaluationService);
        new ApplicationContextProvider().setApplicationContext(context);
    }

    private SujetProjet sujet(StatutSujet statut, Double score) {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setStatut(statut);
        sujet.setScoreFinalEvaluation(score);
        return sujet;
    }

    private long anyId() {
        return 42L;
    }
}

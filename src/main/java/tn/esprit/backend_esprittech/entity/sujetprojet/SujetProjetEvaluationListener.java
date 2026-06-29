package tn.esprit.backend_esprittech.entity.sujetprojet;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.backend_esprittech.config.ApplicationContextProvider;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;

public class SujetProjetEvaluationListener {

    @jakarta.persistence.PostUpdate
    public void onPostUpdate(SujetProjet sujetProjet) {
        if (sujetProjet.getStatut() != StatutSujet.REALISATION_TERMINEE || sujetProjet.getScoreFinalEvaluation() != null) {
            return;
        }

        Runnable task = () -> ApplicationContextProvider.getBean(EvaluationService.class)
                .calculateEvaluationForSystem(sujetProjet.getId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}

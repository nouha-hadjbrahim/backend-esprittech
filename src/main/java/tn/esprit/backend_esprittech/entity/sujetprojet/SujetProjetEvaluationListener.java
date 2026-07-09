package tn.esprit.backend_esprittech.entity.sujetprojet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.backend_esprittech.config.ApplicationContextProvider;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;

public class SujetProjetEvaluationListener {

    private static final Logger log = LoggerFactory.getLogger(SujetProjetEvaluationListener.class);

    @jakarta.persistence.PostUpdate
    public void onPostUpdate(SujetProjet sujetProjet) {
        if (sujetProjet.getStatut() != StatutSujet.REALISATION_TERMINEE || sujetProjet.getScoreFinalEvaluation() != null) {
            return;
        }

        Runnable task = () -> {
            try {
                ApplicationContextProvider.getBean(EvaluationService.class)
                        .calculateEvaluationForSystem(sujetProjet.getId());
            } catch (RuntimeException ex) {
                log.warn("Automatic ML evaluation failed after project termination for sujet {}", sujetProjet.getId(), ex);
            }
        };

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

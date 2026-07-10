package tn.esprit.backend_esprittech.service.CritereEval;

public enum EvaluationTrigger {
    AUTOMATIC_TERMINATION,
    MANUAL_RECALCULATION,
    ADMIN_RECALCULATION,
    CI_REVIEW,
    RETRY_AFTER_FAILURE
}

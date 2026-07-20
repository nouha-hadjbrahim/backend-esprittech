package tn.esprit.backend_esprittech.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.backend_esprittech.dto.CritereEval.CriterionConfigurationIssueResponse;
import tn.esprit.backend_esprittech.dto.request.ChangePasswordRequest;
import tn.esprit.backend_esprittech.dto.request.ResendCodeRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateProfileRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.TerminaisonSujetRequest;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationRunResult;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationTrigger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecordAccessorTest {

    @Test
    void accountRequestRecords_returnValues() {
        ChangePasswordRequest change = new ChangePasswordRequest("oldPass1", "NewPass1");
        UpdateProfileRequest profile = new UpdateProfileRequest("Karim", "Mokaddem");
        ResendCodeRequest resend = new ResendCodeRequest("user@esprit.tn");

        assertEquals("oldPass1", change.currentPassword());
        assertEquals("NewPass1", change.newPassword());
        assertEquals("Karim", profile.nom());
        assertEquals("Mokaddem", profile.prenom());
        assertEquals("user@esprit.tn", resend.email());
    }

    @Test
    void evaluationAndSujetRecords_returnValues() {
        CriterionConfigurationIssueResponse issue = new CriterionConfigurationIssueResponse(
                1L, "Qualite", "NOTE", List.of("poids"), List.of("description"), "v2", "Bloquant");
        TerminaisonSujetRequest terminaison = new TerminaisonSujetRequest("data:image/png;base64,abc", "image/png");
        EvaluationRunResult run = new EvaluationRunResult(
                7L, EvaluationTrigger.MANUAL_RECALCULATION, "corr-1", "hash", "engine-v1", null);

        assertEquals(1L, issue.criterionId());
        assertEquals(List.of("poids"), issue.blockingMissingFields());
        assertEquals("image/png", terminaison.coverImageContentType());
        assertEquals(7L, run.projectId());
        assertEquals(EvaluationTrigger.MANUAL_RECALCULATION, run.trigger());
        assertNull(run.evaluation());
    }
}

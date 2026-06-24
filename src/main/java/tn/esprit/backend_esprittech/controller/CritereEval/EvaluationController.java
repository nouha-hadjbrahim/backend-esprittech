package tn.esprit.backend_esprittech.controller.CritereEval;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.EvaluationResponse;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping("/{id}/evaluations/calculer")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE')")
    public EvaluationResponse calculerEvaluation(
            @PathVariable Long id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return evaluationService.calculerEvaluation(id, request);
    }

    @GetMapping("/{id}/evaluation")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CHEF_EQUIPE')")
    public EvaluationResponse getLatestEvaluation(@PathVariable Long id) {
        return evaluationService.getLatestEvaluationByProjet(id);
    }
}
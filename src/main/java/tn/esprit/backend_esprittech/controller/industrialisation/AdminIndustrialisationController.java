package tn.esprit.backend_esprittech.controller.industrialisation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationResponse;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;
import tn.esprit.backend_esprittech.service.industrialisation.QuestionIndustrialisationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/industrialisation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIndustrialisationController {

    private final QuestionIndustrialisationService questionService;
    private final IndustrialisationService industrialisationService;

    @GetMapping("/questions")
    public List<QuestionIndustrialisationResponse> findQuestions() {
        return questionService.findAll();
    }

    @GetMapping("/questions/actives")
    public List<QuestionIndustrialisationResponse> findActiveQuestions() {
        return questionService.findActive();
    }

    @PostMapping("/questions")
    public QuestionIndustrialisationResponse createQuestion(
            @Valid @RequestBody QuestionIndustrialisationRequest request
    ) {
        return questionService.create(request);
    }

    @PutMapping("/questions/{id}")
    public QuestionIndustrialisationResponse updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionIndustrialisationRequest request
    ) {
        return questionService.update(id, request);
    }

    @PatchMapping("/questions/{id}/activer")
    public ResponseEntity<Void> activateQuestion(@PathVariable Long id) {
        questionService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/questions/{id}/desactiver")
    public ResponseEntity<Void> deactivateQuestion(@PathVariable Long id) {
        questionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<CandidatureIndustrialisationResponse> findAllRequests() {
        return industrialisationService.listForAdmin();
    }
}

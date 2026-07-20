package tn.esprit.backend_esprittech.controller.critereeval;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteResponse;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRuleRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereNoteRuleResponse;
import tn.esprit.backend_esprittech.dto.critereeval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.service.critereeval.CritereNoteService;
import tn.esprit.backend_esprittech.service.critereeval.CritereNoteRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/criteres/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CritereNoteController {

    private final CritereNoteService critereNoteService;
    private final CritereNoteRuleService critereNoteRuleService;

    @GetMapping
    public ResponseEntity<List<CritereNoteResponse>> findAll() {
        return ResponseEntity.ok(critereNoteService.findAll());
    }

    @GetMapping({"/actifs", "/active"})
    public ResponseEntity<List<CritereNoteResponse>> findActive() {
        return ResponseEntity.ok(critereNoteService.findActive());
    }

    @PostMapping
    public ResponseEntity<CritereNoteResponse> create(
            @Valid @RequestBody CritereNoteRequest request
    ) {
        return ResponseEntity.ok(critereNoteService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CritereNoteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CritereNoteRequest request
    ) {
        return ResponseEntity.ok(critereNoteService.update(id, request));
    }

    @PatchMapping({"/{id}/desactiver", "/{id}/deactivate"})
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        critereNoteService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/{id}/activer", "/{id}/activate"})
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        critereNoteService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        critereNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ordre")
    public ResponseEntity<Void> reorder(@Valid @RequestBody List<OrdreCritereRequest> requests) {
        critereNoteService.reorder(requests);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{critereId}/rules")
    public ResponseEntity<List<CritereNoteRuleResponse>> findRules(@PathVariable Long critereId) {
        return ResponseEntity.ok(critereNoteRuleService.findByCritere(critereId));
    }

    @PostMapping("/{critereId}/rules")
    public ResponseEntity<CritereNoteRuleResponse> createRule(
            @PathVariable Long critereId,
            @Valid @RequestBody CritereNoteRuleRequest request
    ) {
        return ResponseEntity.ok(critereNoteRuleService.create(critereId, request));
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<CritereNoteRuleResponse> updateRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody CritereNoteRuleRequest request
    ) {
        return ResponseEntity.ok(critereNoteRuleService.update(ruleId, request));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long ruleId) {
        critereNoteRuleService.delete(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{ruleId}/activer")
    public ResponseEntity<Void> activateRule(@PathVariable Long ruleId) {
        critereNoteRuleService.activate(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{ruleId}/desactiver")
    public ResponseEntity<Void> deactivateRule(@PathVariable Long ruleId) {
        critereNoteRuleService.deactivate(ruleId);
        return ResponseEntity.noContent().build();
    }
}

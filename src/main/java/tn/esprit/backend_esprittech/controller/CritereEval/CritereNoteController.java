package tn.esprit.backend_esprittech.controller.CritereEval;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteResponse;
import tn.esprit.backend_esprittech.dto.CritereEval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.service.CritereEval.CritereNoteService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/criteres/notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CritereNoteController {

    private final CritereNoteService critereNoteService;

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

    @PutMapping("/ordre")
    public ResponseEntity<Void> reorder(@Valid @RequestBody List<OrdreCritereRequest> requests) {
        critereNoteService.reorder(requests);
        return ResponseEntity.noContent().build();
    }
}

package tn.esprit.backend_esprittech.controller.CritereEval;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteRequest;
import tn.esprit.backend_esprittech.dto.CritereEval.CritereNoteResponse;
import tn.esprit.backend_esprittech.service.CritereEval.CritereNoteService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/criteres/notes")
@RequiredArgsConstructor
public class CritereNoteController {

    private final CritereNoteService critereNoteService;

    @GetMapping
    public ResponseEntity<List<CritereNoteResponse>> findAll() {
        return ResponseEntity.ok(critereNoteService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CritereNoteResponse>> findActive() {
        return ResponseEntity.ok(critereNoteService.findActive());
    }

    @PostMapping
    public ResponseEntity<CritereNoteResponse> create(
            @RequestBody CritereNoteRequest request
    ) {
        return ResponseEntity.ok(critereNoteService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CritereNoteResponse> update(
            @PathVariable Long id,
            @RequestBody CritereNoteRequest request
    ) {
        return ResponseEntity.ok(critereNoteService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        critereNoteService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        critereNoteService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
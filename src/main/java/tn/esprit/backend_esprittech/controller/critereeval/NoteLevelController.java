package tn.esprit.backend_esprittech.controller.critereeval;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.backend_esprittech.dto.critereeval.NoteLevelRequest;
import tn.esprit.backend_esprittech.dto.critereeval.NoteLevelResponse;
import tn.esprit.backend_esprittech.service.critereeval.NoteLevelService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evaluation/note-levels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NoteLevelController {

    private final NoteLevelService service;

    @GetMapping
    public List<NoteLevelResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    public NoteLevelResponse create(@Valid @RequestBody NoteLevelRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public NoteLevelResponse update(@PathVariable Long id, @Valid @RequestBody NoteLevelRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/activer")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        service.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desactiver")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

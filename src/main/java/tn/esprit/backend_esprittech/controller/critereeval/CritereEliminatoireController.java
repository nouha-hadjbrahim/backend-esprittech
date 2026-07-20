package tn.esprit.backend_esprittech.controller.critereeval;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireRequest;
import tn.esprit.backend_esprittech.dto.critereeval.CritereEliminatoireResponse;
import tn.esprit.backend_esprittech.dto.critereeval.OrdreCritereRequest;
import tn.esprit.backend_esprittech.service.critereeval.CritereEliminatoireService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/criteres/eliminatoires")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CritereEliminatoireController {

    private final CritereEliminatoireService service;

    @GetMapping
    public List<CritereEliminatoireResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/actifs")
    public List<CritereEliminatoireResponse> findActive() {
        return service.findActive();
    }

    @PostMapping
    public CritereEliminatoireResponse create(@Valid @RequestBody CritereEliminatoireRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CritereEliminatoireResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CritereEliminatoireRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/desactiver")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    @PatchMapping("/{id}/activer")
    public void activate(@PathVariable Long id) {
        service.activate(id);
    }

    @PutMapping("/ordre")
    public void reorder(@Valid @RequestBody List<OrdreCritereRequest> requests) {
        service.reorder(requests);
    }
}
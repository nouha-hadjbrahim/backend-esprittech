package tn.esprit.backend_esprittech.controller.industrialisation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationFormResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.ReponsesIndustrialisationRequest;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IndustrialisationController {

    private final IndustrialisationService industrialisationService;

    @PostMapping("/api/projets/{projetId}/industrialisation")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public CandidatureIndustrialisationResponse create(
            @PathVariable Long projetId,
            @Valid @RequestBody CandidatureIndustrialisationRequest request
    ) {
        return industrialisationService.create(projetId, request);
    }

    @GetMapping("/api/industrialisation/{id}/formulaire")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public IndustrialisationFormResponse formulaire(@PathVariable Long id) {
        return industrialisationService.getFormulaire(id);
    }

    @GetMapping("/api/industrialisation/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENSEIGNANT','CI')")
    public CandidatureIndustrialisationResponse detail(@PathVariable Long id) {
        return industrialisationService.detail(id);
    }

    @PutMapping("/api/industrialisation/{id}/reponses")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public CandidatureIndustrialisationResponse saveReponses(
            @PathVariable Long id,
            @Valid @RequestBody ReponsesIndustrialisationRequest request
    ) {
        return industrialisationService.saveReponses(id, request);
    }

    @PostMapping(value = "/api/industrialisation/{id}/preuves", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public CandidatureIndustrialisationResponse uploadPreuve(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @RequestPart("file") MultipartFile file
    ) {
        return industrialisationService.uploadPreuve(id, questionId, file);
    }

    @PostMapping("/api/industrialisation/{id}/soumettre")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public CandidatureIndustrialisationResponse soumettre(@PathVariable Long id) {
        return industrialisationService.soumettre(id);
    }

    @GetMapping("/api/industrialisation/mes-demandes")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public List<CandidatureIndustrialisationResponse> mesDemandes() {
        return industrialisationService.mesDemandes();
    }
}

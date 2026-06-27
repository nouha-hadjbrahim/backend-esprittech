package tn.esprit.backend_esprittech.controller.industrialisation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationResponse;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionNoGoRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.IndustrialisationScoreResponse;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationService;
import tn.esprit.backend_esprittech.service.industrialisation.IndustrialisationScoreService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ci/industrialisation")
@PreAuthorize("hasRole('CI')")
public class CiIndustrialisationController {

    private final IndustrialisationService industrialisationService;
    private final IndustrialisationScoreService scoreService;

    @GetMapping
    public List<CandidatureIndustrialisationResponse> findRequests(
            @RequestParam(required = false) StatutIndustrialisation statut,
            @RequestParam(required = false) TypeIndustrialisation type
    ) {
        return industrialisationService.listForCi(statut, type);
    }

    @GetMapping("/{id}")
    public CandidatureIndustrialisationResponse detail(@PathVariable Long id) {
        return industrialisationService.detailForCi(id);
    }

    @GetMapping("/{id}/score")
    public IndustrialisationScoreResponse score(@PathVariable Long id) {
        return scoreService.calculate(id);
    }

    @PostMapping("/{id}/go")
    public CandidatureIndustrialisationResponse go(
            @PathVariable Long id,
            @Valid @RequestBody DecisionGoRequest request
    ) {
        return industrialisationService.decideGo(id, request);
    }

    @PostMapping("/{id}/no-go")
    public CandidatureIndustrialisationResponse noGo(
            @PathVariable Long id,
            @Valid @RequestBody DecisionNoGoRequest request
    ) {
        return industrialisationService.decideNoGo(id, request);
    }
}

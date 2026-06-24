package tn.esprit.backend_esprittech.mapper.sujetprojet;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.service.sujetprojet.MockEncadrantContext;

import java.util.Arrays;
import java.util.List;

@Component
public class SujetProjetMapper {

    public SujetProjet toEntity(SujetProjetRequest request) {
        return SujetProjet.builder()
                .titre(request.titre().trim())
                .categorie(request.categorie())
                .description(request.description().trim())
                .objectifs(request.objectifs().trim())
                .prerequis(request.prerequis().trim())
                .domaine(request.domaine().trim())
                .technologies(String.join(",", request.technologies()))
                .capaciteAccueil(request.capaciteAccueil())
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(MockEncadrantContext.ENCADRANT_ID)
                .eligibleIndustrialisation(false)
                .catalogue(false)
                .build();
    }

    public void updateEntity(SujetProjet entity, SujetProjetRequest request) {
        entity.setTitre(request.titre().trim());
        entity.setCategorie(request.categorie());
        entity.setDescription(request.description().trim());
        entity.setObjectifs(request.objectifs().trim());
        entity.setPrerequis(request.prerequis().trim());
        entity.setDomaine(request.domaine().trim());
        entity.setTechnologies(String.join(",", request.technologies()));
        entity.setCapaciteAccueil(request.capaciteAccueil());
    }

    public SujetProjetResponse toResponse(SujetProjet entity) {
        return new SujetProjetResponse(
                entity.getId(),
                entity.getTitre(),
                entity.getCategorie(),
                entity.getDescription(),
                entity.getObjectifs(),
                entity.getPrerequis(),
                entity.getDomaine(),
                parseTechnologies(entity.getTechnologies()),
                entity.getCapaciteAccueil(),
                entity.getStatut(),
                entity.getScoreFinalEvaluation(),
                entity.getEligibleIndustrialisation(),
                entity.getCatalogue(),
                entity.getEncadrantId(),
                entity.getDateCreation(),
                entity.getDateSoumission(),
                entity.getDateValidation(),
                entity.getDateDebutRealisation(),
                entity.getDateTerminaison(),
                entity.getMotifInvalidation()
        );
    }

    private List<String> parseTechnologies(String technologies) {
        if (technologies == null || technologies.isBlank()) {
            return List.of();
        }
        return Arrays.stream(technologies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

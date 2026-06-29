package tn.esprit.backend_esprittech.mapper.sujetprojet;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;

import java.util.Arrays;
import java.util.List;

@Component
public class SujetProjetMapper {

    public SujetProjet toEntity(SujetProjetRequest request, Long encadrantId) {
        return SujetProjet.builder()
                .titre(request.titre().trim())
                .categorie(request.categorie())
                .description(request.description().trim())
                .objectifs(request.objectifs().trim())
                .prerequis(joinValues(request.prerequis()))
                .domaine(joinValues(request.domaines()))
                .technologies(joinValues(request.technologies()))
                .capaciteAccueil(request.capaciteAccueil())
                .statut(StatutSujet.SOUMIS_EN_VALIDATION)
                .encadrantId(encadrantId)
                .eligibleIndustrialisation(false)
                .catalogue(false)
                .build();
    }

    public void updateEntity(SujetProjet entity, SujetProjetRequest request) {
        entity.setTitre(request.titre().trim());
        entity.setCategorie(request.categorie());
        entity.setDescription(request.description().trim());
        entity.setObjectifs(request.objectifs().trim());
        entity.setPrerequis(joinValues(request.prerequis()));
        entity.setDomaine(joinValues(request.domaines()));
        entity.setTechnologies(joinValues(request.technologies()));
        entity.setCapaciteAccueil(request.capaciteAccueil());
    }

    public SujetProjetResponse toResponse(SujetProjet entity, String encadrantNom,
                                          String encadrantEmail, String equipeNom) {
        return new SujetProjetResponse(
                entity.getId(),
                entity.getTitre(),
                entity.getCategorie(),
                entity.getDescription(),
                entity.getObjectifs(),
                parseValues(entity.getPrerequis()),
                parseValues(entity.getDomaine()),
                parseValues(entity.getTechnologies()),
                entity.getCapaciteAccueil(),
                entity.getStatut(),
                entity.getScoreFinalEvaluation(),
                entity.getEligibleIndustrialisation(),
                entity.getCatalogue(),
                entity.getEncadrantId(),
                encadrantNom,
                encadrantEmail,
                equipeNom,
                entity.getDateCreation(),
                entity.getDateSoumission(),
                entity.getDateValidation(),
                entity.getDateDebutRealisation(),
                entity.getDateTerminaison(),
                entity.getMotifInvalidation()
        );
    }

    private String joinValues(List<String> values) {
        return String.join(",", values.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private List<String> parseValues(String values) {
        if (values == null || values.isBlank()) {
            return List.of();
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}

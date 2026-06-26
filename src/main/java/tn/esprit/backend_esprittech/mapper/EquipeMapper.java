package tn.esprit.backend_esprittech.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;

/**
 * Conversion des entités {@link EquipeRecherche} vers leur représentation API
 * et inversement.
 */
@Component
public class EquipeMapper {

    private final UserMapper userMapper;

    public EquipeMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** Construit une entité {@link EquipeRecherche} depuis le DTO de création. */
    public EquipeRecherche toEntity(CreateEquipeRechercheRequest request) {
        if (request == null) {
            return null;
        }
        var builder = EquipeRecherche.builder()
                .nom(request.nom())
                .description(request.description())
                .domaine(request.domaine());
        if (request.statut() != null) {
            builder.statut(request.statut());
        }
        return builder.build();
    }

    /** Met à jour partiellement une entité existante depuis le DTO de mise à jour. */
    public void updateEntity(UpdateEquipeRechercheRequest request, EquipeRecherche equipe) {
        if (request == null || equipe == null) {
            return;
        }
        if (request.nom() != null) {
            equipe.setNom(request.nom());
        }
        if (request.description() != null) {
            equipe.setDescription(request.description());
        }
        if (request.domaine() != null) {
            equipe.setDomaine(request.domaine());
        }
        if (request.statut() != null) {
            equipe.setStatut(request.statut());
        }
    }

    /** Convertit une entité vers sa représentation API (avec nombre de membres). */
    public EquipeResponse toResponse(EquipeRecherche equipe, int nbMembres) {
        if (equipe == null) {
            return null;
        }
        String statutLabel = equipe.getStatut() != null ? equipe.getStatut().getDisplayName() : "Actif";
        return new EquipeResponse(
                equipe.getId(),
                equipe.getNom(),
                equipe.getDescription(),
                equipe.getDomaine(),
                userMapper.toResponse(equipe.getChef()),
                equipe.getMembers().stream().map(userMapper::toResponse).toList(),
                nbMembres,
                equipe.getCreatedAt(),
                statutLabel
        );
    }
}
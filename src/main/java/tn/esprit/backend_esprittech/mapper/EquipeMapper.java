package tn.esprit.backend_esprittech.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;

/**
 * Conversion des entités {@link EquipeRecherche} vers leur représentation API.
 */
@Component
public class EquipeMapper {

    private final UserMapper userMapper;

    public EquipeMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public EquipeResponse toResponse(EquipeRecherche equipe, int nbMembres) {
        if (equipe == null) {
            return null;
        }
        return new EquipeResponse(
                equipe.getId(),
                equipe.getNom(),
                equipe.getDescription(),
                userMapper.toResponse(equipe.getChef()),
                nbMembres,
                equipe.getCreatedAt()
        );
    }
}

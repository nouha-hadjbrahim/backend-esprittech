package tn.esprit.backend_esprittech.mapper.candidature;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.candidature.response.LivrableResponse;
import tn.esprit.backend_esprittech.entity.candidature.Livrable;

@Component
public class LivrableMapper {

    public LivrableResponse toResponse(Livrable l) {
        return new LivrableResponse(
                l.getId(),
                l.getSujetId(),
                l.getType(),
                l.getNom(),
                l.getLien(),
                l.getDescription(),
                l.isVisible(),
                l.getDeposant().getId(),
                l.getDeposant().getNom(),
                l.getDateDepot()
        );
    }
}

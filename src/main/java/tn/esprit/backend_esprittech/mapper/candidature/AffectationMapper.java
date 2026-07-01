package tn.esprit.backend_esprittech.mapper.candidature;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;

@Component
public class AffectationMapper {

    public AffectationResponse toResponse(Affectation a) {
        return new AffectationResponse(
                a.getId(),
                a.getSujetId(),
                a.getEtudiant().getId(),
                a.getEtudiant().getNom(),
                a.getEtudiant().getPrenom(),
                a.getEtudiant().getEmail(),
                a.getStatut(),
                a.getDateDebut(),
                a.getDateRetrait(),
                a.getMotifRetrait()
        );
    }
}

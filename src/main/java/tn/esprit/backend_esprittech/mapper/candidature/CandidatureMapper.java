package tn.esprit.backend_esprittech.mapper.candidature;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;

@Component
public class CandidatureMapper {

    public CandidatureResponse toResponse(Candidature c) {
        return new CandidatureResponse(
                c.getId(),
                c.getSujetId(),
                c.getEtudiant().getId(),
                c.getEtudiant().getNom(),
                c.getEtudiant().getPrenom(),
                c.getStatut(),
                c.getMotifRefus(),
                c.getMessageEtudiant(),
                c.getDateDepot(),
                c.getDateDecision()
        );
    }
}

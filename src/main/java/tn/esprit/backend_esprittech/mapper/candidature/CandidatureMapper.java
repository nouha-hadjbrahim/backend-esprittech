package tn.esprit.backend_esprittech.mapper.candidature;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;

@Component
public class CandidatureMapper {

    public CandidatureResponse toResponse(Candidature c) {
        return toResponse(c, null, null);
    }

    public CandidatureResponse toResponse(Candidature c, SujetProjet sujet, User encadrant) {
        return new CandidatureResponse(
                c.getId(),
                c.getSujetId(),
                c.getEtudiant().getId(),
                c.getEtudiant().getNom(),
                c.getEtudiant().getPrenom(),
                c.getEtudiant().getEmail(),
                c.getStatut(),
                c.getMotifRefus(),
                c.getMessageEtudiant(),
                c.getDateDepot(),
                c.getDateDecision(),
                sujet != null ? sujet.getTitre() : null,
                sujet != null && sujet.getCategorie() != null ? sujet.getCategorie().name() : null,
                formatNom(encadrant)
        );
    }

    private String formatNom(User user) {
        if (user == null) {
            return null;
        }
        return user.getPrenom() + " " + user.getNom();
    }
}

package tn.esprit.backend_esprittech.dto.CritereEval;


import java.time.LocalDateTime;

public record CritereNoteResponse(
        Long id,
        String libelle,
        String description,
        String domaine,
        Integer ordre,
        Boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour,
        Double bareme,
        Double poids,
        Double seuil
) {
}
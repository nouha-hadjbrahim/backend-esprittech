package tn.esprit.backend_esprittech.dto.CritereEval;


import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;

import java.time.LocalDateTime;

public record CritereEliminatoireResponse(
        Long id,
        String libelle,
        String description,
        String domaine,
        Integer ordre,
        Boolean actif,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour,
        ReponseEliminatoire reponseAttendue
) {
}
package tn.esprit.backend_esprittech.dto.request;

import java.util.List;

public record UpdateResearchTeamRequest(
        String nom,
        String domaineRecherche,
        String description,
        Long chefId,
        List<Long> memberIds
) {
}
package tn.esprit.backend_esprittech.dto.response;

import java.util.List;

public record ResearchTeamResponse(
        Long id,
        String nom,
        String domaineRecherche,
        String description,
        ResearchTeamUserResponse chef,
        List<ResearchTeamUserResponse> members
) {
}
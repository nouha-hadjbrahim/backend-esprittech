package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateResearchTeamRequest(

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le domaine de recherche est obligatoire")
        String domaineRecherche,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotNull(message = "Le chef est obligatoire")
        Long chefId,

        List<Long> memberIds
) {
}
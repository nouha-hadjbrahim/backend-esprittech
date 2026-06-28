package tn.esprit.backend_esprittech.dto.candidature.request;

import jakarta.validation.constraints.NotNull;

public record CandidatureRequest(
        @NotNull(message = "L'identifiant du sujet est obligatoire")
        Long sujetId,

        String messageEtudiant
) {}

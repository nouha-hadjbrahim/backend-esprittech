package tn.esprit.backend_esprittech.dto.candidature.request;

import jakarta.validation.constraints.NotNull;

public record DecisionCandidatureRequest(
        @NotNull(message = "La décision est obligatoire (true = accepter, false = refuser)")
        Boolean accepter,

        /** Obligatoire si accepter = false (RG-08) */
        String motifRefus
) {}

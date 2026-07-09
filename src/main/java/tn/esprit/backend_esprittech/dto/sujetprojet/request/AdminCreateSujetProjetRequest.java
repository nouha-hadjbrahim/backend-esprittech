package tn.esprit.backend_esprittech.dto.sujetprojet.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AdminCreateSujetProjetRequest(
        @NotNull(message = "L'encadrant est obligatoire")
        Long encadrantId,

        @NotNull(message = "Les informations du sujet sont obligatoires")
        @Valid
        SujetProjetRequest sujet
) {
}

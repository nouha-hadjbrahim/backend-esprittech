package tn.esprit.backend_esprittech.dto.industrialisation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReponsesIndustrialisationRequest(
        @NotNull List<@Valid ReponseIndustrialisationRequest> reponses
) {
}

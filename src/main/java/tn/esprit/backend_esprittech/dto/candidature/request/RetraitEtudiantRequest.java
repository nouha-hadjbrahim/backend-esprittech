package tn.esprit.backend_esprittech.dto.candidature.request;

import jakarta.validation.constraints.NotBlank;

public record RetraitEtudiantRequest(
        @NotBlank(message = "Le motif de retrait est obligatoire")
        String motifRetrait
) {}

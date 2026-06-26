package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AjouterMembresRequest(
        @NotEmpty(message = "La liste des membres est obligatoire")
        List<Long> memberIds
) {
}

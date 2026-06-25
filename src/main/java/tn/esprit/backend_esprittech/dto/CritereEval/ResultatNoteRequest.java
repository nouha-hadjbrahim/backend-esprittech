package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.NotNull;

public record ResultatNoteRequest(
        @NotNull Long critereId,
        @NotNull Double noteObtenue,
        String commentaire
) {
}
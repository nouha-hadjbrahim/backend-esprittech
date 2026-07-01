package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.constraints.NotNull;

public record ResultatNoteRequest(
        @NotNull Long critereId,
        Double noteObtenue,
        Integer noteValue,
        String commentaire
) {
    public ResultatNoteRequest(Long critereId, Double noteObtenue, String commentaire) {
        this(critereId, noteObtenue, noteObtenue == null ? null : noteObtenue.intValue(), commentaire);
    }
}

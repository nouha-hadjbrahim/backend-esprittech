package tn.esprit.backend_esprittech.dto.CritereEval;


import jakarta.validation.Valid;

import java.util.List;

public record EvaluationRequest(
        @Valid List<ResultatEliminatoireRequest> eliminatoires,
        @Valid List<ResultatNoteRequest> notes
) {
}
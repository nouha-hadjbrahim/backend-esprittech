package tn.esprit.backend_esprittech.dto.critereeval;

import java.util.List;
import java.util.Map;

public record MlExternalSourceRequest(
        String sourceType,
        String reference,
        String authorisationTokenReference,
        List<Map<String, String>> files,
        String content
) {
}

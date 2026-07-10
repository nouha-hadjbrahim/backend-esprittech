package tn.esprit.backend_esprittech.dto.industrialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.backend_esprittech.dto.response.HistoriqueResponse;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.enums.Role;

import java.time.LocalDateTime;
import java.util.Map;

public record HistoriqueIndustrialisationResponse(
        Long id,
        StatutIndustrialisation statutSource,
        StatutIndustrialisation statutCible,
        Long acteurId,
        String acteurNom,
        Role roleActeur,
        LocalDateTime dateTransition,
        String motif,
        String commentaire
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static HistoriqueIndustrialisationResponse from(HistoriqueResponse hr) {
        StatutIndustrialisation statutSource = null;
        StatutIndustrialisation statutCible = null;
        String motif = null;
        String commentaire = null;

        try {
            if (hr.oldValues() != null) {
                Map<String, Object> oldMap = MAPPER.readValue(hr.oldValues(), Map.class);
                if (oldMap.containsKey("statut")) {
                    statutSource = StatutIndustrialisation.valueOf((String) oldMap.get("statut"));
                }
            }
            if (hr.newValues() != null) {
                Map<String, Object> newMap = MAPPER.readValue(hr.newValues(), Map.class);
                if (newMap.containsKey("statut")) {
                    statutCible = StatutIndustrialisation.valueOf((String) newMap.get("statut"));
                }
            }
            if (hr.metadata() != null) {
                Map<String, Object> metaMap = MAPPER.readValue(hr.metadata(), Map.class);
                if (metaMap.containsKey("motif")) {
                    motif = (String) metaMap.get("motif");
                }
                if (metaMap.containsKey("commentaire")) {
                    commentaire = (String) metaMap.get("commentaire");
                }
            }
        } catch (Exception ignored) {
        }

        return new HistoriqueIndustrialisationResponse(
                hr.id(),
                statutSource,
                statutCible,
                hr.actorId(),
                hr.actorPrenom() + " " + hr.actorNom(),
                hr.actorRole(),
                hr.createdAt(),
                motif,
                commentaire
        );
    }
}

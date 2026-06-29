package tn.esprit.backend_esprittech.enums.equipe;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatutAffiliation {
    EN_ATTENTE("En attente"),
    ACCEPTEE("Acceptée"),
    REFUSEE("Refusée");

    private final String displayName;

    StatutAffiliation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static StatutAffiliation fromValue(String value) {
        if (value == null) return null;
        for (StatutAffiliation s : values()) {
            if (s.name().equalsIgnoreCase(value) || s.displayName.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid StatutAffiliation: " + value);
    }
}

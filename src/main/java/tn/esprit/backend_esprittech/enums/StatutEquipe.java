package tn.esprit.backend_esprittech.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StatutEquipe {
    ACTIF("Actif"),
    INACTIF("Inactif");

    private final String displayName;

    StatutEquipe(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static StatutEquipe fromValue(String value) {
        if (value == null) return null;
        for (StatutEquipe s : values()) {
            if (s.displayName.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid StatutEquipe: " + value);
    }
}

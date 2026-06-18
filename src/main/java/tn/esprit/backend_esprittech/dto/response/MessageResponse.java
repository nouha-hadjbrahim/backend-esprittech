package tn.esprit.backend_esprittech.dto.response;

import java.time.Instant;


public record MessageResponse(
        String message,
        Instant timestamp
) {
    public static MessageResponse of(String message) {
        return new MessageResponse(message, Instant.now());
    }
}

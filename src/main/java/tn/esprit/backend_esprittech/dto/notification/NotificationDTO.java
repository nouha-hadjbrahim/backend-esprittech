package tn.esprit.backend_esprittech.dto.notification;

import tn.esprit.backend_esprittech.enums.NotificationType;

import java.time.Instant;

public record NotificationDTO(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        Instant createdAt
) {
}

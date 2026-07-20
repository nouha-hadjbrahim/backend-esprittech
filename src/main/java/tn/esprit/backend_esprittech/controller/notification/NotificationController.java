package tn.esprit.backend_esprittech.controller.notification;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.backend_esprittech.dto.notification.NotificationDTO;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<NotificationDTO> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page) {
        return notificationService.getParUtilisateur(user.getId(), page);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal User user) {
        return Map.of("count", notificationService.getNombreNonLues(user.getId()));
    }

    @GetMapping("/unread")
    public List<NotificationDTO> getUnreadByType(
            @AuthenticationPrincipal User user,
            @RequestParam NotificationType type) {
        return notificationService.getNonLuesParType(user.getId(), type);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id, @AuthenticationPrincipal User user) {
        notificationService.marquerCommeLu(id, user.getId());
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.marquerToutCommeLu(user.getId());
    }
}

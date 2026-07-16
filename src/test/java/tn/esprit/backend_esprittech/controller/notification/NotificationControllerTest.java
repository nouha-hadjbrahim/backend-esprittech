package tn.esprit.backend_esprittech.controller.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import tn.esprit.backend_esprittech.dto.notification.NotificationDTO;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.NotificationType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.service.notification.NotificationService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks private NotificationController controller;

    private User user;
    private NotificationDTO dto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@esprit.tn")
                .role(Role.ROLE_ETUDIANT)
                .typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true)
                .build();
        dto = new NotificationDTO(
                1L, NotificationType.CANDIDATURE_RECUE,
                "Candidature recue", "Un etudiant a depose une candidature.",
                "/frontoffice/mes-projets", false,
                Instant.parse("2026-07-15T10:00:00Z"));
    }

    @Test
    void getNotifications_returnsPage() {
        Page<NotificationDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(notificationService.getParUtilisateur(10L, 0)).thenReturn(page);

        Page<NotificationDTO> result = controller.getNotifications(user, 0);

        assertEquals(1, result.getContent().size());
        assertEquals(NotificationType.CANDIDATURE_RECUE, result.getContent().get(0).type());
    }

    @Test
    void getNotifications_defaultPageIsZero() {
        Page<NotificationDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
        when(notificationService.getParUtilisateur(10L, 0)).thenReturn(page);

        Page<NotificationDTO> result = controller.getNotifications(user, 0);

        assertEquals(0, result.getNumber());
    }

    @Test
    void getNotifications_emptyPage() {
        Page<NotificationDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationService.getParUtilisateur(10L, 0)).thenReturn(emptyPage);

        Page<NotificationDTO> result = controller.getNotifications(user, 0);

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void getUnreadCount_returnsCount() {
        when(notificationService.getNombreNonLues(10L)).thenReturn(7L);

        Map<String, Long> result = controller.getUnreadCount(user);

        assertEquals(7L, result.get("count"));
    }

    @Test
    void getUnreadCount_zeroUnread() {
        when(notificationService.getNombreNonLues(10L)).thenReturn(0L);

        Map<String, Long> result = controller.getUnreadCount(user);

        assertEquals(0L, result.get("count"));
    }

    @Test
    void markAsRead_delegatesToService() {
        controller.markAsRead(1L, user);

        verify(notificationService).marquerCommeLu(1L, 10L);
    }

    @Test
    void markAsRead_differentId() {
        controller.markAsRead(99L, user);

        verify(notificationService).marquerCommeLu(99L, 10L);
    }

    @Test
    void markAllAsRead_delegatesToService() {
        controller.markAllAsRead(user);

        verify(notificationService).marquerToutCommeLu(10L);
    }

    @Test
    void getNotifications_page2() {
        Page<NotificationDTO> page = new PageImpl<>(List.of(dto), PageRequest.of(2, 20), 45);
        when(notificationService.getParUtilisateur(10L, 2)).thenReturn(page);

        Page<NotificationDTO> result = controller.getNotifications(user, 2);

        assertEquals(2, result.getNumber());
    }
}

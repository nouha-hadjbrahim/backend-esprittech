package tn.esprit.backend_esprittech.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;


@Component
public class UserMapper {

    /**
     * Conversion sans information d'affiliation (utilisée pour les listes admin et équipe,
     * où l'appartenance à une équipe de recherche n'est pas pertinente).
     */
    public UserResponse toResponse(User user) {
        return toResponse(user, false, null, null);
    }

    /**
     * Conversion enrichie de l'affiliation à une équipe de recherche
     * (utilisée pour le profil courant /auth/me et les réponses d'authentification).
     */
    public UserResponse toResponse(User user, boolean isAffilieToEquipe, Long equipeId, String equipeNom) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getNom(),
                user.getPrenom(),
                user.getEmail(),
                user.getIdentifiant(),
                user.getRole(),
                user.getTypeUtilisateur(),
                user.getDepartement(),
                user.isEnabled(),
                user.getCreatedAt(),
                isAffilieToEquipe,
                equipeId,
                equipeNom
        );
    }
}

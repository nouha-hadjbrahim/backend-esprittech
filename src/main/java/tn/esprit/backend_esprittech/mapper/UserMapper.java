package tn.esprit.backend_esprittech.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;


@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
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
                user.isEnabled()
        );
    }
}

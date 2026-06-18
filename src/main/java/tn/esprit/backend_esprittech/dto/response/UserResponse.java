package tn.esprit.backend_esprittech.dto.response;

import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

public record UserResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String identifiant,
        Role role,
        TypeUtilisateur typeUtilisateur,
        String departement,
        boolean enabled
) {
}

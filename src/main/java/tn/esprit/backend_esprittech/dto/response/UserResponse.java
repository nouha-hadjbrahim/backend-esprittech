package tn.esprit.backend_esprittech.dto.response;

import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;

import java.time.Instant;

public record UserResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        Role role,
        TypeUtilisateur typeUtilisateur,
        String departement,
        boolean enabled,
        Instant createdAt,
        boolean isAffilieToEquipe,
        Long equipeId,
        String equipeNom
) {

}

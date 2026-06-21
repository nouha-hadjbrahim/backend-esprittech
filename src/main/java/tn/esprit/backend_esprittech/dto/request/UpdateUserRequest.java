package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.enums.Role;

/**
 * Mise à jour d'un utilisateur par l'admin (identité, rôle, état du compte).
 */
public record UpdateUserRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotNull(message = "Le rôle est obligatoire")
        Role role,

        boolean enabled
) {
}

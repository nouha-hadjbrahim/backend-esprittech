package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tn.esprit.backend_esprittech.enums.Role;

/**
 * Création directe d'un utilisateur par l'admin (sans passer par le référentiel).
 * Mêmes contraintes de saisie que l'inscription self-service.
 */
public record CreateUserRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Pattern(regexp = ".+@esprit\\.tn$", message = "L'email doit se terminer par @esprit.tn")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins 1 majuscule, 1 minuscule et 1 chiffre")
        String password,

        @NotNull(message = "Le rôle est obligatoire")
        Role role,

        boolean enabled
) {
}

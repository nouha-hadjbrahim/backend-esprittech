package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changement du mot de passe par l'utilisateur lui-même (vérification de l'ancien mot de passe requise).
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins 1 majuscule, 1 minuscule et 1 chiffre")
        String newPassword
) {
}

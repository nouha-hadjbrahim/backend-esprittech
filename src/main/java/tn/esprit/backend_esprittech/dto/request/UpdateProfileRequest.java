package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mise à jour du profil par l'utilisateur lui-même (l'email et le rôle ne sont pas modifiables ici).
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
        String prenom
) {
}

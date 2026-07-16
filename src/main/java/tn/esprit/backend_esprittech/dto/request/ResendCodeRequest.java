package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Demande de renvoi du code de vérification pour une inscription en attente. */
public record ResendCodeRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Pattern(regexp = ".+@esprit\\.tn$", message = "L'email doit se terminer par @esprit.tn")
        String email
) {
}

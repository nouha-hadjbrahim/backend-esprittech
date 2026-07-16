package tn.esprit.backend_esprittech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Validation du code OTP envoyé à l'email lors de l'inscription. */
public record VerifyEmailRequest(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Pattern(regexp = ".+@esprit\\.tn$", message = "L'email doit se terminer par @esprit.tn")
        String email,

        @NotBlank(message = "Le code est obligatoire")
        @Pattern(regexp = "\\d{6}", message = "Le code doit contenir 6 chiffres")
        String code
) {
}

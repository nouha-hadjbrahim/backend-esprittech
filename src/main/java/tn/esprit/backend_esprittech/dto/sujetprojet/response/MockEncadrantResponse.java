package tn.esprit.backend_esprittech.dto.sujetprojet.response;

public record MockEncadrantResponse(
        Long id,
        String nomComplet,
        String role,
        String email,
        String departement
) {
}

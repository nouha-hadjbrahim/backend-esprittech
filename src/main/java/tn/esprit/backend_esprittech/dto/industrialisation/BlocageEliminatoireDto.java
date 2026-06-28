package tn.esprit.backend_esprittech.dto.industrialisation;

public record BlocageEliminatoireDto(
        String source,
        Long referenceId,
        String libelle,
        String raison
) {
}

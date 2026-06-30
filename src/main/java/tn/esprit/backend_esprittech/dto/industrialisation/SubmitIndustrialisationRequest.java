package tn.esprit.backend_esprittech.dto.industrialisation;

public record SubmitIndustrialisationRequest(
        Boolean confirmEliminatoryWarnings
) {
    public boolean isConfirmed() {
        return Boolean.TRUE.equals(confirmEliminatoryWarnings);
    }
}

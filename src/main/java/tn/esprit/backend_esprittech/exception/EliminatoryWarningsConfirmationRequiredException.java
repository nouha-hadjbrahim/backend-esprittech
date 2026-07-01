package tn.esprit.backend_esprittech.exception;

import tn.esprit.backend_esprittech.dto.industrialisation.BlocageEliminatoireDto;

import java.util.List;

public class EliminatoryWarningsConfirmationRequiredException extends RuntimeException {

    private final int count;
    private final List<BlocageEliminatoireDto> details;

    public EliminatoryWarningsConfirmationRequiredException(int count, List<BlocageEliminatoireDto> details) {
        super("Etes-vous sur de continuer ? Vous avez " + count
                + " critere(s) eliminatoire(s) non satisfait(s). Si vous continuez, la candidature sera envoyee au CI avec ces alertes.");
        this.count = count;
        this.details = details;
    }

    public int getCount() {
        return count;
    }

    public List<BlocageEliminatoireDto> getDetails() {
        return details;
    }
}

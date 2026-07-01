package tn.esprit.backend_esprittech.repository.CritereEval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;

import java.util.List;
import java.util.Optional;

public interface CritereNoteRepository extends JpaRepository<CritereNote, Long> {

    List<CritereNote> findByActifTrueOrderByOrdreAsc();

    List<CritereNote> findByActifTrueAndDomaineOrderByOrdreAsc(String domaine);

    Optional<CritereNote> findByLibelleIgnoreCase(String libelle);

    boolean existsByDefaultNoteValue(Integer defaultNoteValue);
}

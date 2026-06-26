package tn.esprit.backend_esprittech.repository.CritereEval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereNote;

import java.util.List;

public interface CritereNoteRepository extends JpaRepository<CritereNote, Long> {

    List<CritereNote> findByActifTrueOrderByOrdreAsc();

    List<CritereNote> findByActifTrueAndDomaineOrderByOrdreAsc(String domaine);
}
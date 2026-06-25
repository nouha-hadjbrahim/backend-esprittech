package tn.esprit.backend_esprittech.repository.CritereEval;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.CritereEliminatoire;

import java.util.List;

public interface CritereEliminatoireRepository extends JpaRepository<CritereEliminatoire, Long> {

    List<CritereEliminatoire> findByActifTrueOrderByOrdreAsc();
}
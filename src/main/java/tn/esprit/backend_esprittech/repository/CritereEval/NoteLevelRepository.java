package tn.esprit.backend_esprittech.repository.CritereEval;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.backend_esprittech.entity.CritereEval.NoteLevel;

import java.util.List;
import java.util.Optional;

public interface NoteLevelRepository extends JpaRepository<NoteLevel, Long> {

    List<NoteLevel> findByActiveTrueOrderByOrderIndexAsc();

    List<NoteLevel> findAllByOrderByOrderIndexAsc();

    Optional<NoteLevel> findByValue(Integer value);

    boolean existsByValue(Integer value);
}

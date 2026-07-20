package tn.esprit.backend_esprittech.repository.critereeval;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import tn.esprit.backend_esprittech.entity.critereeval.Evaluation;
import tn.esprit.backend_esprittech.entity.critereeval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.critereeval.ResultatCritere;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.enums.TypeProjet;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogue_eval_status;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EvaluationRepositoryCatalogueStatusTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ProjetCatalogueRepository projetRepository;
    @Autowired private EvaluationRepository evaluationRepository;

    @Test
    void persistedCatalogueEvaluation_shouldSurviveProjectStatusChangesUnchanged() {
        ProjetCatalogue projet = persistedCatalogueProject();
        Evaluation evaluation = persistedEvaluation(projet);

        Long projetId = projet.getId();
        Double score = evaluation.getScoreFinal();
        LocalDateTime dateCalcul = evaluation.getDateCalcul();
        List<CriterionSnapshot> details = snapshot(evaluation);
        int historyCount = evaluationRepository.findByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetId).size();

        for (StatutProjet statut : List.of(
                StatutProjet.CANDIDAT_INDUSTRIALISATION_INTERNE,
                StatutProjet.INDUSTRIALISE_DSI
        )) {
            ProjetCatalogue reloadedProjet = projetRepository.findById(projetId).orElseThrow();
            reloadedProjet.setStatut(statut);
            projetRepository.saveAndFlush(reloadedProjet);
            entityManager.clear();

            ProjetCatalogue afterStatusUpdate = projetRepository.findById(projetId).orElseThrow();
            Evaluation latest = evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetId)
                    .orElseThrow();
            List<Evaluation> history = evaluationRepository.findByProjetCatalogueIdOrderByDateCalculDescIdDesc(projetId);

            assertEquals(statut, afterStatusUpdate.getStatut());
            assertEquals(88, afterStatusUpdate.getScore());
            assertEquals(historyCount, history.size());
            assertEquals(evaluation.getId(), latest.getId());
            assertEquals(score, latest.getScoreFinal());
            assertEquals(dateCalcul, latest.getDateCalcul());
            assertEquals(details, snapshot(latest));
        }
    }

    private ProjetCatalogue persistedCatalogueProject() {
        EquipeDomaine domaine = EquipeDomaine.builder()
                .nom("Industrialisation")
                .build();
        entityManager.persist(domaine);

        User chef = user("chef-status@esprit.tn", Role.ROLE_CHEF_EQUIPE);
        entityManager.persist(chef);

        EquipeRecherche equipe = EquipeRecherche.builder()
                .nom("Equipe Evaluation Status")
                .description("Equipe de test")
                .domaine(domaine)
                .chef(chef)
                .build();
        entityManager.persist(equipe);

        User encadrant = user("encadrant-status@esprit.tn", Role.ROLE_ENSEIGNANT);
        encadrant.setEquipeRecherche(equipe);
        entityManager.persist(encadrant);

        ProjetCatalogue projet = ProjetCatalogue.builder()
                .typeProjet(TypeProjet.RDI)
                .titre("Projet catalogue evalue")
                .description("Description complete")
                .objectifs("Objectifs complets")
                .dateDebut(LocalDate.of(2026, 1, 1))
                .dateFin(LocalDate.of(2026, 12, 31))
                .statut(StatutProjet.VALIDE)
                .score(88)
                .encadrant(encadrant)
                .equipeRecherche(equipe)
                .build();
        return projetRepository.saveAndFlush(projet);
    }

    private Evaluation persistedEvaluation(ProjetCatalogue projet) {
        Evaluation evaluation = new Evaluation();
        evaluation.setProjetCatalogue(projet);
        evaluation.setScoreFinal(88.0);
        evaluation.setMlScore(88.0);
        evaluation.setEligibleIndustrialisation(true);
        evaluation.setBloqueParEliminatoire(false);
        evaluation.setHasEliminatoryWarnings(false);
        evaluation.setEliminatoryWarningsCount(0);
        evaluation.setValidationStatus("PENDING");
        evaluation.setMlStatus("COMPLETED");
        evaluation.setProcessingStatus("COMPLETED");
        evaluation.setEligibilityStatus("ELIGIBLE");
        evaluation.setMlGlobalConfidence(0.91);
        evaluation.setRecommendations("Maintenir les preuves de deploiement");

        ResultatCritere note = new ResultatCritere();
        note.setCritereId(20L);
        note.setCritereLibelle("Qualite technique");
        note.setTypeCritere(TypeCritere.NOTE);
        note.setNoteObtenue(4.4);
        note.setPoids(2.0);
        note.setScorePondere(17.6);
        note.setRuleConfigured(true);
        note.setCriterionStatus("SCORED");
        note.setExplanation("Evidence-conditioned score");
        note.setEvidenceSummary("Architecture et tests presents");
        note.setConfidence(0.87);
        note.setRecommendations("Ajouter une preuve de deploiement");
        evaluation.addResultat(note);

        ResultatCritere eliminatoire = new ResultatCritere();
        eliminatoire.setCritereId(10L);
        eliminatoire.setCritereLibelle("Documentation disponible");
        eliminatoire.setTypeCritere(TypeCritere.ELIMINATOIRE);
        eliminatoire.setReponseEliminatoire(ReponseEliminatoire.OK);
        eliminatoire.setRuleConfigured(true);
        eliminatoire.setCriterionStatus("OK");
        eliminatoire.setEvidenceSummary("Guide utilisateur present");
        eliminatoire.setConfidence(0.93);
        evaluation.addResultat(eliminatoire);

        Evaluation saved = evaluationRepository.saveAndFlush(evaluation);
        assertNotNull(saved.getDateCalcul());
        entityManager.clear();
        return evaluationRepository.findTopByProjetCatalogueIdOrderByDateCalculDescIdDesc(projet.getId())
                .orElseThrow();
    }

    private List<CriterionSnapshot> snapshot(Evaluation evaluation) {
        return evaluation.getResultats().stream()
                .map(resultat -> new CriterionSnapshot(
                        resultat.getCritereId(),
                        resultat.getCritereLibelle(),
                        resultat.getTypeCritere(),
                        resultat.getReponseEliminatoire(),
                        resultat.getNoteObtenue(),
                        resultat.getPoids(),
                        resultat.getScorePondere(),
                        resultat.getCriterionStatus(),
                        resultat.getExplanation(),
                        resultat.getEvidenceSummary(),
                        resultat.getConfidence(),
                        resultat.getRecommendations()
                ))
                .sorted(Comparator.comparing(CriterionSnapshot::critereId))
                .toList();
    }

    private User user(String email, Role role) {
        return User.builder()
                .nom("Test")
                .prenom("User")
                .email(email)
                .password("password")
                .role(role)
                .typeUtilisateur(TypeUtilisateur.ENSEIGNANT)
                .enabled(true)
                .build();
    }

    private record CriterionSnapshot(
            Long critereId,
            String critereLibelle,
            TypeCritere typeCritere,
            ReponseEliminatoire reponseEliminatoire,
            Double noteObtenue,
            Double poids,
            Double scorePondere,
            String criterionStatus,
            String explanation,
            String evidenceSummary,
            Double confidence,
            String recommendations
    ) {
    }
}

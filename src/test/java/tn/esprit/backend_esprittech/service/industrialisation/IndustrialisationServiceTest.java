package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.CandidatureIndustrialisationRequest;
import tn.esprit.backend_esprittech.dto.industrialisation.DecisionNoGoRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableResponse;
import tn.esprit.backend_esprittech.entity.CritereEval.ReponseEliminatoire;
import tn.esprit.backend_esprittech.entity.CritereEval.TypeCritere;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.industrialisation.CandidatureIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.ReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.StatutIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.repository.industrialisation.CandidatureIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.HistoriqueIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.industrialisation.ReponseIndustrialisationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.CritereEval.EvaluationService;
import tn.esprit.backend_esprittech.service.livrable.LivrableService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndustrialisationServiceTest {

    @Mock
    private CandidatureIndustrialisationRepository candidatureRepository;

    @Mock
    private QuestionIndustrialisationRepository questionRepository;

    @Mock
    private ReponseIndustrialisationRepository reponseRepository;

    @Mock
    private HistoriqueIndustrialisationRepository historiqueRepository;

    @Mock
    private SujetProjetRepository sujetProjetRepository;

    @Mock
    private UserService userService;

    @Mock
    private EvaluationService evaluationService;

    @Mock
    private LivrableService livrableService;

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private IndustrialisationService industrialisationService;

    private User encadrant;
    private User ci;
    private SujetProjet projet;
    private CandidatureIndustrialisation candidature;

    @BeforeEach
    void setUp() {
        encadrant = new User();
        encadrant.setId(7L);
        encadrant.setPrenom("Ali");
        encadrant.setNom("Mansour");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        ci = new User();
        ci.setId(9L);
        ci.setPrenom("Comite");
        ci.setNom("Industrie");
        ci.setRole(Role.ROLE_CI);

        projet = new SujetProjet();
        projet.setId(42L);
        projet.setTitre("Plateforme IoT");
        projet.setEncadrantId(7L);
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);
        projet.setScoreFinalEvaluation(76.0);
        projet.setEligibleIndustrialisation(true);
        projet.setBloqueParEliminatoire(false);

        candidature = new CandidatureIndustrialisation();
        candidature.setId(11L);
        candidature.setSujetProjet(projet);
        candidature.setDemandeur(encadrant);
        candidature.setTypeIndustrialisation(TypeIndustrialisation.INTERNE);
        candidature.setStatut(StatutIndustrialisation.BROUILLON);
    }

    @Test
    void soumettre_nonEligibleProject_shouldBlockIndustrialisation() {
        projet.setEligibleIndustrialisation(false);
        projet.setBloqueParEliminatoire(true);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Projet non eligible a l'industrialisation: critere eliminatoire non conforme.", ex.getMessage());
    }

    @Test
    void create_existingDraftRequest_shouldReopenDraft() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of(candidature));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(historiqueRepository.findByCandidatureIndustrialisationIdOrderByDateTransitionAsc(11L)).thenReturn(List.of());
        when(livrableService.findByProjet(42L)).thenReturn(List.of());

        var response = industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.EXTERNE, null));

        assertEquals(11L, response.id());
        assertEquals(StatutIndustrialisation.BROUILLON, response.statut());
    }

    @Test
    void create_existingSubmittedRequest_shouldRejectDuplicate() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(candidatureRepository.findBySujetProjetIdOrderByDateDemandeDesc(42L)).thenReturn(List.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.create(42L, new CandidatureIndustrialisationRequest(TypeIndustrialisation.EXTERNE, null)));

        assertEquals("Une demande d'industrialisation est deja en cours pour ce projet.", ex.getMessage());
    }

    @Test
    void soumettre_requiredFileQuestionWithoutProof_shouldRejectIncompleteForm() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(5L);
        question.setLibelle("Preuve de depot");
        question.setTypeReponse(TypeReponseIndustrialisation.FICHIER);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(1);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Reponse obligatoire manquante: Preuve de depot", ex.getMessage());
    }

    @Test
    void soumettre_requiredBooleanEliminatoryAnswer_shouldSubmitWhenPersisted() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setValeurBoolean(true);
        reponse.setReponseEliminatoire(ReponseEliminatoire.OK);
        reponse.setJustificatif("Depot Git fourni");

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
    }

    @Test
    void soumettre_requiredBooleanWithoutValue_shouldReturnPreciseError() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setReponseEliminatoire(ReponseEliminatoire.OK);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.soumettre(11L));

        assertEquals("Reponse booleenne manquante pour: Depot Git disponible ?", ex.getMessage());
    }

    @Test
    void soumettre_requiredEliminatoryWithoutOkNotOk_shouldComputeAutomatically() {
        QuestionIndustrialisation question = gitQuestion();
        ReponseIndustrialisation reponse = new ReponseIndustrialisation();
        reponse.setId(22L);
        reponse.setCandidatureIndustrialisation(candidature);
        reponse.setQuestion(question);
        reponse.setValeurBoolean(true);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(livrableService.hasActiveLivrables(42L)).thenReturn(true);
        when(questionRepository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of(reponse));
        when(sujetProjetRepository.save(projet)).thenReturn(projet);
        when(candidatureRepository.save(any(CandidatureIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = industrialisationService.soumettre(11L);

        assertEquals(StatutIndustrialisation.SOUMISE, response.statut());
    }

    @Test
    void decideNoGo_blankMotif_shouldRequireMotif() {
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> industrialisationService.decideNoGo(11L, new DecisionNoGoRequest("  ")));

        assertEquals("Le motif est obligatoire pour une decision No Go.", ex.getMessage());
    }

    @Test
    void detailForCi_shouldExposeLatestProjectScore() {
        projet.setScoreFinalEvaluation(91.0);
        candidature.setScoreEvaluationProjet(55.0);
        candidature.setStatut(StatutIndustrialisation.SOUMISE);
        when(userService.getCurrentUser()).thenReturn(ci);
        when(candidatureRepository.findById(11L)).thenReturn(Optional.of(candidature));
        when(reponseRepository.findByCandidatureIndustrialisationId(11L)).thenReturn(List.of());
        when(historiqueRepository.findByCandidatureIndustrialisationIdOrderByDateTransitionAsc(11L)).thenReturn(List.of());
        when(livrableService.findByProjet(42L)).thenReturn(List.of(
                new LivrableResponse(1L, 42L, "Plateforme IoT", null, "Rapport", null, null, null, null, null, null, 7L, "Ali Mansour", null, true)
        ));

        var response = industrialisationService.detailForCi(11L);

        assertEquals(91.0, response.scoreEvaluationProjet());
    }

    private QuestionIndustrialisation gitQuestion() {
        QuestionIndustrialisation question = new QuestionIndustrialisation();
        question.setId(5L);
        question.setLibelle("Depot Git disponible ?");
        question.setTypeReponse(TypeReponseIndustrialisation.BOOLEAN);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setObligatoire(true);
        question.setActif(true);
        question.setOrdre(1);
        return question;
    }
}

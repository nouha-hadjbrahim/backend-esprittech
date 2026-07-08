package tn.esprit.backend_esprittech.service.candidature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.*;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.*;
import tn.esprit.backend_esprittech.mapper.candidature.AffectationMapper;
import tn.esprit.backend_esprittech.mapper.candidature.CandidatureMapper;
import tn.esprit.backend_esprittech.repository.*;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidatureMapper candidatureMapper;
    @Mock private AffectationMapper affectationMapper;
    @Mock private UserService userService;
    @Mock private SujetProjetService sujetProjetService;

    @InjectMocks
    private CandidatureService candidatureService;

    private User etudiant;
    private User encadrant;
    private SujetProjet sujetOuvert;
    private SujetProjet sujetValide;
    private SujetProjet sujetEnRealisation;
    private Candidature candidatureDeposee;
    private Affectation affectationActive;

    @BeforeEach
    void setUp() {
        etudiant = new User();
        etudiant.setId(1L);
        etudiant.setNom("Ben Ali");
        etudiant.setPrenom("Sami");
        etudiant.setRole(Role.ROLE_ETUDIANT);

        encadrant = new User();
        encadrant.setId(2L);
        encadrant.setNom("Trabelsi");
        encadrant.setPrenom("Nadia");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        sujetValide = new SujetProjet();
        sujetValide.setId(10L);
        sujetValide.setStatut(StatutSujet.VALIDE);
        sujetValide.setEncadrantId(2L);

        sujetOuvert = new SujetProjet();
        sujetOuvert.setId(10L);
        sujetOuvert.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        sujetOuvert.setEncadrantId(2L);

        sujetEnRealisation = new SujetProjet();
        sujetEnRealisation.setId(10L);
        sujetEnRealisation.setStatut(StatutSujet.REALISATION_EN_COURS);
        sujetEnRealisation.setEncadrantId(2L);

        lenient().when(userService.getCurrentUser()).thenReturn(encadrant);

        candidatureDeposee = Candidature.builder()
                .sujetId(10L)
                .etudiant(etudiant)
                .statut(StatutCandidature.DEPOSEE)
                .build();
        candidatureDeposee = setId(candidatureDeposee, 100L);

        affectationActive = Affectation.builder()
                .sujetId(10L)
                .etudiant(etudiant)
                .candidature(candidatureDeposee)
                .statut(StatutAffectation.ACTIVE)
                .build();
        affectationActive = setAffectationId(affectationActive, 200L);
    }

    // ── US-14 : Ouvrir candidatures ──────────────────────────────────
    @Test
    void ouvrirCandidatures_sujetValide_passeCandidatureOuverte() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));
        when(sujetProjetRepository.save(any())).thenReturn(sujetValide);

        candidatureService.ouvrirCandidatures(10L);

        assertEquals(StatutSujet.CANDIDATURE_OUVERTE, sujetValide.getStatut());
        verify(sujetProjetRepository).save(sujetValide);
    }

    @Test
    void ouvrirCandidatures_sujetNonValide_leveException() {
        sujetValide.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));

        assertThrows(IllegalStateException.class, () -> candidatureService.ouvrirCandidatures(10L));
    }

    // ── US-16 : Dépôt de candidature ────────────────────────────────
    @Test
    void deposerCandidature_sujetOuvert_creeCandidate() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndEtudiantId(10L, 1L)).thenReturn(Optional.empty());
        when(candidatureRepository.save(any())).thenReturn(candidatureDeposee);
        when(candidatureMapper.toResponse(any())).thenReturn(mockCandidatureResponse());

        CandidatureResponse result = candidatureService.deposerCandidature(
                new CandidatureRequest(10L, "Je suis motivé"));

        assertNotNull(result);
        verify(candidatureRepository).save(any(Candidature.class));
    }

    @Test
    void deposerCandidature_sujetFerme_leveException() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.deposerCandidature(new CandidatureRequest(10L, "Je candidate")));

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void deposerCandidature_doublonExistant_leveException() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndEtudiantId(10L, 1L)).thenReturn(Optional.of(candidatureDeposee));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.deposerCandidature(new CandidatureRequest(10L, "Je candidate")));

        verify(candidatureRepository, never()).save(any());
    }

    // ── US-17 / US-18 : Acceptation & Refus ───────────────────────
    @Test
    void deciderCandidature_acceptation_creeAffectation() {
        Candidature autreCandidature = setId(Candidature.builder()
                .sujetId(11L)
                .etudiant(etudiant)
                .statut(StatutCandidature.DEPOSEE)
                .build(), 101L);

        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findByEtudiantIdAndStatut(1L, StatutCandidature.DEPOSEE))
                .thenReturn(List.of(candidatureDeposee, autreCandidature));
        when(candidatureRepository.save(candidatureDeposee)).thenReturn(candidatureDeposee);
        when(candidatureMapper.toResponse(candidatureDeposee)).thenReturn(mockCandidatureResponse());

        CandidatureResponse result = candidatureService.deciderCandidature(
                100L,
                new DecisionCandidatureRequest(true, null));

        assertNotNull(result);
        assertEquals(StatutCandidature.ACCEPTEE, candidatureDeposee.getStatut());
        assertNotNull(candidatureDeposee.getDateDecision());
        assertEquals(StatutCandidature.ARCHIVEE, autreCandidature.getStatut());
        assertNotNull(autreCandidature.getDateDecision());
        verify(affectationRepository).save(any(Affectation.class));
        verify(candidatureRepository).saveAll(List.of(autreCandidature));
    }

    @Test
    void deciderCandidature_refusAvecMotif_enregistreMotif() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.save(candidatureDeposee)).thenReturn(candidatureDeposee);
        when(candidatureMapper.toResponse(candidatureDeposee)).thenReturn(mockCandidatureResponse());

        CandidatureResponse result = candidatureService.deciderCandidature(
                100L,
                new DecisionCandidatureRequest(false, "Profil non retenu"));

        assertNotNull(result);
        assertEquals(StatutCandidature.REFUSEE, candidatureDeposee.getStatut());
        assertEquals("Profil non retenu", candidatureDeposee.getMotifRefus());
        assertNotNull(candidatureDeposee.getDateDecision());
        verify(affectationRepository, never()).save(any());
    }

    @Test
    void deciderCandidature_refusSansMotif_leveException() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalArgumentException.class,
                () -> candidatureService.deciderCandidature(100L, new DecisionCandidatureRequest(false, "  ")));
    }

    @Test
    void deciderCandidature_dejaTraitee_leveException() {
        candidatureDeposee.setStatut(StatutCandidature.ACCEPTEE);
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.deciderCandidature(100L, new DecisionCandidatureRequest(true, null)));
    }

    // ── US-19 + US-20 : Fermeture + Réalisation ───────────────────
    @Test
    void fermerCandidatures_sujetOuvert_basculeSurRealisationEnCours() {
        Candidature enAttente = setId(Candidature.builder()
                .sujetId(10L)
                .etudiant(etudiant)
                .statut(StatutCandidature.DEPOSEE)
                .build(), 102L);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndStatut(10L, StatutCandidature.DEPOSEE))
                .thenReturn(List.of(enAttente));

        candidatureService.fermerCandidatures(10L);

        assertEquals(StatutSujet.REALISATION_EN_COURS, sujetOuvert.getStatut());
        assertEquals(StatutCandidature.ARCHIVEE, enAttente.getStatut());
        verify(candidatureRepository).saveAll(List.of(enAttente));
        verify(sujetProjetRepository).save(sujetOuvert);
    }

    @Test
    void fermerCandidatures_sujetNonOuvert_leveException() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));

        assertThrows(IllegalStateException.class, () -> candidatureService.fermerCandidatures(10L));
    }

    // ── US-22 : Retrait étudiant ─────────────────────────────────────
    @Test
    void retirerEtudiant_affectationActive_archiveAvecMotif() {
        when(affectationRepository.findById(200L)).thenReturn(Optional.of(affectationActive));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));
        when(affectationRepository.save(affectationActive)).thenReturn(affectationActive);
        when(affectationMapper.toResponse(affectationActive)).thenReturn(mockAffectationResponse());

        AffectationResponse result = candidatureService.retirerEtudiant(
                200L,
                new RetraitEtudiantRequest("Abandon sans justification"));

        assertNotNull(result);
        assertEquals(StatutAffectation.RETIREE_ARCHIVEE, affectationActive.getStatut());
        assertEquals("Abandon sans justification", affectationActive.getMotifRetrait());
        assertNotNull(affectationActive.getDateRetrait());
    }

    @Test
    void retirerEtudiant_affectationDejaArchivee_leveException() {
        affectationActive.setStatut(StatutAffectation.RETIREE_ARCHIVEE);
        when(affectationRepository.findById(200L)).thenReturn(Optional.of(affectationActive));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.retirerEtudiant(200L, new RetraitEtudiantRequest("Retrait")));
    }

    @Test
    void retirerEtudiant_sujetPasEnRealisation_leveException() {
        when(affectationRepository.findById(200L)).thenReturn(Optional.of(affectationActive));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.retirerEtudiant(200L, new RetraitEtudiantRequest("Retrait")));
    }

    // ── US-24 : Terminaison ──────────────────────────────────────────
    @Test
    void declarerTerminaison_sujetEnRealisation_passeTermine() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));

        candidatureService.declarerTerminaison(10L);

        assertEquals(StatutSujet.REALISATION_TERMINEE, sujetEnRealisation.getStatut());
        verify(sujetProjetRepository).save(sujetEnRealisation);
    }

    @Test
    void declarerTerminaison_sujetPasEnRealisation_leveException() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalStateException.class, () -> candidatureService.declarerTerminaison(10L));
    }

    @Test
    void getCandidaturesParSujet_mappeLesResponses() {
        when(candidatureRepository.findBySujetIdOrderByDateDepotAsc(10L)).thenReturn(List.of(candidatureDeposee));
        when(candidatureMapper.toResponse(candidatureDeposee)).thenReturn(mockCandidatureResponse());

        List<CandidatureResponse> result = candidatureService.getCandidaturesParSujet(10L);

        assertEquals(1, result.size());
        verify(candidatureMapper).toResponse(candidatureDeposee);
    }

    @Test
    void getMesCandidatures_utiliseUtilisateurCourant() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(candidatureRepository.findByEtudiantIdOrderByDateDepotDesc(1L)).thenReturn(List.of(candidatureDeposee));
        when(sujetProjetRepository.findAllById(List.of(10L))).thenReturn(List.of(sujetOuvert));
        when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(encadrant));
        when(candidatureMapper.toResponse(candidatureDeposee, sujetOuvert, encadrant))
                .thenReturn(mockCandidatureResponse());

        List<CandidatureResponse> result = candidatureService.getMesCandidatures();

        assertEquals(1, result.size());
    }

    @Test
    void retirerMaCandidature_deposee_supprime() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));

        candidatureService.retirerMaCandidature(100L);

        verify(candidatureRepository).delete(candidatureDeposee);
    }

    @Test
    void retirerMaCandidature_autreEtudiant_interdit() {
        User autre = user(Role.ROLE_ETUDIANT, 99L);
        when(userService.getCurrentUser()).thenReturn(autre);
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));

        assertThrows(AccessDeniedException.class, () -> candidatureService.retirerMaCandidature(100L));
    }

    @Test
    void retirerMaCandidature_dejaTraitee_interdit() {
        candidatureDeposee.setStatut(StatutCandidature.ACCEPTEE);
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));

        assertThrows(IllegalStateException.class, () -> candidatureService.retirerMaCandidature(100L));
    }

    @Test
    void getAffectationsParSujet_mappeLesResponses() {
        when(sujetProjetService.getSujetById(10L)).thenReturn(new SujetProjetResponse(
                10L, "Sujet", CategorieSujet.PFE, null, null, List.of(), List.of(), List.of(), 2,
                StatutSujet.CANDIDATURE_OUVERTE, null, false, false, 2L, null, null, null,
                null, null, null, null, null, null, false, 0, 0));
        when(affectationRepository.findBySujetIdAndStatut(10L, StatutAffectation.ACTIVE))
                .thenReturn(List.of(affectationActive));
        when(affectationMapper.toResponse(affectationActive)).thenReturn(mockAffectationResponse());

        List<AffectationResponse> result = candidatureService.getAffectationsParSujet(10L);

        assertEquals(1, result.size());
        verify(sujetProjetService).getSujetById(10L);
        verify(affectationMapper).toResponse(affectationActive);
    }

    @Test
    void ouvrirCandidatures_adminPeutGererSujetNonPossede() {
        when(userService.getCurrentUser()).thenReturn(user(Role.ROLE_ADMIN, 99L));
        sujetValide.setEncadrantId(2L);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));

        candidatureService.ouvrirCandidatures(10L);

        assertEquals(StatutSujet.CANDIDATURE_OUVERTE, sujetValide.getStatut());
        assertTrue(sujetValide.getCatalogue());
    }

    @Test
    void fermerCandidatures_chefEquipePeutGererSujetNonPossede() {
        when(userService.getCurrentUser()).thenReturn(user(Role.ROLE_CHEF_EQUIPE, 44L));
        sujetOuvert.setEncadrantId(2L);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndStatut(10L, StatutCandidature.DEPOSEE)).thenReturn(List.of());

        candidatureService.fermerCandidatures(10L);

        assertEquals(StatutSujet.REALISATION_EN_COURS, sujetOuvert.getStatut());
    }

    @Test
    void ouvrirCandidatures_enseignantNonProprietaire_refuse() {
        when(userService.getCurrentUser()).thenReturn(user(Role.ROLE_ENSEIGNANT, 77L));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetValide));

        assertThrows(AccessDeniedException.class, () -> candidatureService.ouvrirCandidatures(10L));
    }

    @Test
    void ouvrirCandidatures_sujetInexistant_leveException() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> candidatureService.ouvrirCandidatures(10L));
    }

    @Test
    void deciderCandidature_inexistante_leveException() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> candidatureService.deciderCandidature(100L, new DecisionCandidatureRequest(true, null)));
    }

    @Test
    void retirerEtudiant_affectationInexistante_leveException() {
        when(affectationRepository.findById(200L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> candidatureService.retirerEtudiant(200L, new RetraitEtudiantRequest("Retrait")));
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsCandidatureFields() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));

        var result = candidatureService.getOldValues(100L);

        assertEquals(candidatureDeposee.getStatut(), result.get("statut"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────
    private CandidatureResponse mockCandidatureResponse() {
        return new CandidatureResponse(100L, 10L, 1L, "Ben Ali", "Sami", "sami@esprit.tn",
                StatutCandidature.DEPOSEE, null, null,
                java.time.Instant.parse("2026-06-25T10:00:00Z"), null,
                "Sujet test", "PFE", "Nadia Trabelsi");
    }

    private AffectationResponse mockAffectationResponse() {
        return new AffectationResponse(200L, 10L, 1L, "Ben Ali", "Sami", "sami@esprit.tn",
                StatutAffectation.RETIREE_ARCHIVEE,
                java.time.Instant.parse("2026-06-25T10:00:00Z"),
                java.time.Instant.parse("2026-06-25T11:00:00Z"),
                "Abandon sans justification");
    }

    private User user(Role role, Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Candidature setId(Candidature c, Long id) {
        try {
            var f = Candidature.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignored) {}
        return c;
    }

    private Affectation setAffectationId(Affectation a, Long id) {
        try {
            var f = Affectation.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, id);
        } catch (Exception ignored) {}
        return a;
    }
}

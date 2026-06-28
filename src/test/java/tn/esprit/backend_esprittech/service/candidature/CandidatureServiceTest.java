package tn.esprit.backend_esprittech.service.candidature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.LivrableRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.LivrableResponse;
import tn.esprit.backend_esprittech.entity.*;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.candidature.Livrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.*;
import tn.esprit.backend_esprittech.mapper.candidature.AffectationMapper;
import tn.esprit.backend_esprittech.mapper.candidature.CandidatureMapper;
import tn.esprit.backend_esprittech.mapper.candidature.LivrableMapper;
import tn.esprit.backend_esprittech.repository.*;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.candidature.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {

    @Mock private CandidatureRepository candidatureRepository;
    @Mock private AffectationRepository affectationRepository;
    @Mock private LivrableRepository livrableRepository;
    @Mock private SujetProjetRepository sujetProjetRepository;
    @Mock private UserRepository userRepository;
    @Mock private CandidatureMapper candidatureMapper;
    @Mock private AffectationMapper affectationMapper;
    @Mock private LivrableMapper livrableMapper;
    @Mock private UserService userService;

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

        // Par défaut, l'utilisateur courant est l'encadrant propriétaire du sujet.
        // Les tests côté étudiant écrasent ce stub avec `etudiant`.
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

        assertThrows(IllegalStateException.class,
                () -> candidatureService.ouvrirCandidatures(10L));
    }

    // ── US-16 : Dépôt de candidature ────────────────────────────────

    @Test
    void deposerCandidature_sujetOuvert_creeCandidate() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndEtudiantId(10L, 1L)).thenReturn(Optional.empty());
        when(candidatureRepository.save(any())).thenReturn(candidatureDeposee);
        CandidatureResponse expected = mockCandidatureResponse();
        when(candidatureMapper.toResponse(any())).thenReturn(expected);

        CandidatureResponse result = candidatureService.deposerCandidature(
                new CandidatureRequest(10L, "Je suis motivé"));

        assertNotNull(result);
        verify(candidatureRepository).save(any(Candidature.class));
    }

    @Test
    void deposerCandidature_sujetFerme_leveException() {
        sujetOuvert.setStatut(StatutSujet.CANDIDATURE_FERMEE);
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.deposerCandidature(new CandidatureRequest(10L, null)));
    }

    @Test
    void deposerCandidature_doublonExistant_leveException() {
        when(userService.getCurrentUser()).thenReturn(etudiant);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndEtudiantId(10L, 1L))
                .thenReturn(Optional.of(candidatureDeposee));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.deposerCandidature(new CandidatureRequest(10L, null)));
    }

    // ── US-17 : Acceptation ──────────────────────────────────────────

    @Test
    void deciderCandidature_acceptation_creeAffectation() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.save(any())).thenReturn(candidatureDeposee);
        when(affectationRepository.save(any())).thenReturn(affectationActive);
        when(candidatureMapper.toResponse(any())).thenReturn(mockCandidatureResponse());

        CandidatureResponse result = candidatureService.deciderCandidature(100L,
                new DecisionCandidatureRequest(true, null));

        assertEquals(StatutCandidature.ACCEPTEE, candidatureDeposee.getStatut());
        verify(affectationRepository).save(any(Affectation.class));
        assertNotNull(result);
    }

    // ── US-18 : Refus ────────────────────────────────────────────────

    @Test
    void deciderCandidature_refusAvecMotif_enregistreMotif() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.save(any())).thenReturn(candidatureDeposee);
        when(candidatureMapper.toResponse(any())).thenReturn(mockCandidatureResponse());

        candidatureService.deciderCandidature(100L,
                new DecisionCandidatureRequest(false, "Profil ne correspond pas"));

        assertEquals(StatutCandidature.REFUSEE, candidatureDeposee.getStatut());
        assertEquals("Profil ne correspond pas", candidatureDeposee.getMotifRefus());
        verify(affectationRepository, never()).save(any());
    }

    @Test
    void deciderCandidature_refusSansMotif_leveException() {
        when(candidatureRepository.findById(100L)).thenReturn(Optional.of(candidatureDeposee));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalArgumentException.class,
                () -> candidatureService.deciderCandidature(100L,
                        new DecisionCandidatureRequest(false, null)));
    }

    // ── US-19 + US-20 : Fermeture + bascule réalisation ─────────────

    @Test
    void fermerCandidatures_sujetOuvert_basculeSurRealisationEnCours() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));
        when(candidatureRepository.findBySujetIdAndStatut(10L, StatutCandidature.DEPOSEE))
                .thenReturn(List.of());
        when(sujetProjetRepository.save(any())).thenReturn(sujetOuvert);

        candidatureService.fermerCandidatures(10L);

        assertEquals(StatutSujet.REALISATION_EN_COURS, sujetOuvert.getStatut());
    }

    @Test
    void fermerCandidatures_sujetNonOuvert_leveException() {
        sujetOuvert.setStatut(StatutSujet.VALIDE);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetOuvert));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.fermerCandidatures(10L));
    }

    // ── US-22 : Retrait étudiant ─────────────────────────────────────

    @Test
    void retirerEtudiant_affectationActive_archiveAvecMotif() {
        when(affectationRepository.findById(200L)).thenReturn(Optional.of(affectationActive));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));
        when(affectationRepository.save(any())).thenReturn(affectationActive);
        when(affectationMapper.toResponse(any())).thenReturn(mockAffectationResponse());

        AffectationResponse result = candidatureService.retirerEtudiant(200L,
                new RetraitEtudiantRequest("Abandon sans justification"));

        assertEquals(StatutAffectation.RETIREE_ARCHIVEE, affectationActive.getStatut());
        assertEquals("Abandon sans justification", affectationActive.getMotifRetrait());
        assertNotNull(affectationActive.getDateRetrait());
        assertNotNull(result);
    }

    @Test
    void retirerEtudiant_affectationDejaArchivee_leveException() {
        affectationActive.setStatut(StatutAffectation.RETIREE_ARCHIVEE);
        when(affectationRepository.findById(200L)).thenReturn(Optional.of(affectationActive));
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.retirerEtudiant(200L,
                        new RetraitEtudiantRequest("motif")));
    }

    // ── US-23 : Livrables ────────────────────────────────────────────

    @Test
    void ajouterLivrable_sujetEnRealisation_enregistreLivrable() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));
        when(userService.getCurrentUser()).thenReturn(encadrant);
        Livrable livrable = Livrable.builder().sujetId(10L).nom("Rapport final")
                .lien("https://git.esprit.tn/repo").type(TypeLivrable.DOCUMENTATION).build();
        when(livrableRepository.save(any())).thenReturn(livrable);
        when(livrableMapper.toResponse(any())).thenReturn(mockLivrableResponse());

        LivrableResponse result = candidatureService.ajouterLivrable(10L,
                new LivrableRequest(TypeLivrable.DOCUMENTATION, "Rapport final",
                        "https://git.esprit.tn/repo", null, true));

        assertNotNull(result);
        verify(livrableRepository).save(any(Livrable.class));
    }

    @Test
    void ajouterLivrable_sujetNonEnRealisation_leveException() {
        sujetEnRealisation.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.ajouterLivrable(10L,
                        new LivrableRequest(TypeLivrable.CODE_SOURCE, "Code", "url", null, true)));
    }

    // ── US-24 : Terminaison ──────────────────────────────────────────

    @Test
    void declarerTerminaison_sujetEnRealisation_passeTermine() {
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));
        when(sujetProjetRepository.save(any())).thenReturn(sujetEnRealisation);

        candidatureService.declarerTerminaison(10L);

        assertEquals(StatutSujet.REALISATION_TERMINEE, sujetEnRealisation.getStatut());
    }

    @Test
    void declarerTerminaison_sujetPasEnRealisation_leveException() {
        sujetEnRealisation.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        when(sujetProjetRepository.findById(10L)).thenReturn(Optional.of(sujetEnRealisation));

        assertThrows(IllegalStateException.class,
                () -> candidatureService.declarerTerminaison(10L));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private CandidatureResponse mockCandidatureResponse() {
        return new CandidatureResponse(100L, 10L, 1L, "Ben Ali", "Sami",
                StatutCandidature.DEPOSEE, null, null,
                java.time.Instant.parse("2026-06-25T10:00:00Z"), null);
    }

    private AffectationResponse mockAffectationResponse() {
        return new AffectationResponse(200L, 10L, 1L, "Ben Ali", "Sami",
                StatutAffectation.RETIREE_ARCHIVEE,
                java.time.Instant.parse("2026-06-25T10:00:00Z"),
                java.time.Instant.parse("2026-06-25T11:00:00Z"),
                "Abandon sans justification");
    }

    private LivrableResponse mockLivrableResponse() {
        return new LivrableResponse(1L, 10L, TypeLivrable.DOCUMENTATION,
                "Rapport final", "https://git.esprit.tn/repo", null, true,
                2L, "Trabelsi", java.time.Instant.parse("2026-06-25T10:00:00Z"));
    }

    /** Reflection helper — sets private id field on Candidature (no setter, set by JPA) */
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

package tn.esprit.backend_esprittech.service.candidature;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.LivrableRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.LivrableResponse;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.candidature.Livrable;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.StatutAffectation;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.mapper.candidature.AffectationMapper;
import tn.esprit.backend_esprittech.mapper.candidature.CandidatureMapper;
import tn.esprit.backend_esprittech.mapper.candidature.LivrableMapper;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.candidature.LivrableRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.Instant;
import java.util.List;

/**
 * Service Candidature — couvre US-14, US-16, US-17, US-18, US-19, US-20, US-22, US-23, US-24.
 */
@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final AffectationRepository affectationRepository;
    private final LivrableRepository livrableRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserRepository userRepository;
    private final CandidatureMapper candidatureMapper;
    private final AffectationMapper affectationMapper;
    private final LivrableMapper livrableMapper;
    private final UserService userService;

    // ─────────────────────────────────────────────────────────────────
    // US-14 : Encadrant ouvre les candidatures d'un sujet validé
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void ouvrirCandidatures(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        if (sujet.getStatut() != StatutSujet.VALIDE) {
            throw new IllegalStateException(
                    "Seul un sujet Validé peut être ouvert aux candidatures. Statut actuel : " + sujet.getStatut());
        }
        sujet.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        sujetProjetRepository.save(sujet);
    }

    // ─────────────────────────────────────────────────────────────────
    // US-16 : Étudiant dépose une candidature
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public CandidatureResponse deposerCandidature(CandidatureRequest request) {
        User etudiant = userService.getCurrentUser();

        SujetProjet sujet = getSujetOrThrow(request.sujetId());
        if (sujet.getStatut() != StatutSujet.CANDIDATURE_OUVERTE) {
            throw new IllegalStateException("Le sujet n'est pas ouvert aux candidatures.");
        }

        candidatureRepository.findBySujetIdAndEtudiantId(request.sujetId(), etudiant.getId())
                .ifPresent(c -> {
                    throw new IllegalStateException("Vous avez déjà déposé une candidature pour ce sujet.");
                });

        Candidature candidature = Candidature.builder()
                .sujetId(request.sujetId())
                .etudiant(etudiant)
                .messageEtudiant(request.messageEtudiant())
                .build();

        return candidatureMapper.toResponse(candidatureRepository.save(candidature));
    }

    // ─────────────────────────────────────────────────────────────────
    // US-17 & US-18 : Encadrant accepte ou refuse une candidature
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public CandidatureResponse deciderCandidature(Long candidatureId, DecisionCandidatureRequest request) {
        Candidature candidature = getCandidatureOrThrow(candidatureId);

        if (candidature.getStatut() != StatutCandidature.DEPOSEE) {
            throw new IllegalStateException("Seule une candidature Déposée peut être traitée.");
        }

        if (request.accepter()) {
            candidature.setStatut(StatutCandidature.ACCEPTEE);
            candidature.setDateDecision(Instant.now());

            Affectation affectation = Affectation.builder()
                    .sujetId(candidature.getSujetId())
                    .etudiant(candidature.getEtudiant())
                    .candidature(candidature)
                    .build();
            affectationRepository.save(affectation);

        } else {
            if (request.motifRefus() == null || request.motifRefus().isBlank()) {
                throw new IllegalArgumentException("Le motif de refus est obligatoire.");
            }
            candidature.setStatut(StatutCandidature.REFUSEE);
            candidature.setMotifRefus(request.motifRefus());
            candidature.setDateDecision(Instant.now());
        }

        return candidatureMapper.toResponse(candidatureRepository.save(candidature));
    }

    // ─────────────────────────────────────────────────────────────────
    // US-19 + US-20 : Fermer candidatures + bascule automatique réalisation
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void fermerCandidatures(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        if (sujet.getStatut() != StatutSujet.CANDIDATURE_OUVERTE) {
            throw new IllegalStateException(
                    "Seul un sujet en CandidatureOuverte peut être fermé. Statut actuel : " + sujet.getStatut());
        }

        // US-20 : transition automatique CandidatureFermée → RéalisationEnCours (RG-06)
        sujet.setStatut(StatutSujet.REALISATION_EN_COURS);

        // Archiver les candidatures encore DEPOSEE
        List<Candidature> enAttente = candidatureRepository
                .findBySujetIdAndStatut(sujetId, StatutCandidature.DEPOSEE);
        enAttente.forEach(c -> c.setStatut(StatutCandidature.ARCHIVEE));
        candidatureRepository.saveAll(enAttente);

        sujetProjetRepository.save(sujet);
    }

    // ─────────────────────────────────────────────────────────────────
    // US-22 : Retrait d'un étudiant pendant la réalisation (archivage)
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public AffectationResponse retirerEtudiant(Long affectationId, RetraitEtudiantRequest request) {
        Affectation affectation = getAffectationOrThrow(affectationId);

        if (affectation.getStatut() != StatutAffectation.ACTIVE) {
            throw new IllegalStateException("Cette affectation est déjà archivée.");
        }

        SujetProjet sujet = getSujetOrThrow(affectation.getSujetId());
        if (sujet.getStatut() != StatutSujet.REALISATION_EN_COURS) {
            throw new IllegalStateException("Le retrait n'est possible que pendant la phase de réalisation.");
        }

        affectation.setStatut(StatutAffectation.RETIREE_ARCHIVEE);
        affectation.setDateRetrait(Instant.now());
        affectation.setMotifRetrait(request.motifRetrait());

        return affectationMapper.toResponse(affectationRepository.save(affectation));
    }

    // ─────────────────────────────────────────────────────────────────
    // US-23 : Dépôt de livrables
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public LivrableResponse ajouterLivrable(Long sujetId, LivrableRequest request) {
        SujetProjet sujet = getSujetOrThrow(sujetId);

        if (sujet.getStatut() != StatutSujet.REALISATION_EN_COURS
                && sujet.getStatut() != StatutSujet.REALISATION_TERMINEE) {
            throw new IllegalStateException(
                    "Les livrables ne peuvent être déposés que pendant la réalisation ou à la terminaison.");
        }

        User deposant = userService.getCurrentUser();

        Livrable livrable = Livrable.builder()
                .sujetId(sujetId)
                .type(request.type())
                .nom(request.nom())
                .lien(request.lien())
                .description(request.description())
                .visible(request.visible())
                .deposant(deposant)
                .build();

        return livrableMapper.toResponse(livrableRepository.save(livrable));
    }

    // ─────────────────────────────────────────────────────────────────
    // US-24 : Déclaration de terminaison
    // ─────────────────────────────────────────────────────────────────

    @Transactional
    public void declarerTerminaison(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        if (sujet.getStatut() != StatutSujet.REALISATION_EN_COURS) {
            throw new IllegalStateException(
                    "La terminaison ne peut être déclarée que depuis RéalisationEnCours.");
        }
        sujet.setStatut(StatutSujet.REALISATION_TERMINEE);
        sujetProjetRepository.save(sujet);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lectures
    // ─────────────────────────────────────────────────────────────────

    public List<CandidatureResponse> getCandidaturesParSujet(Long sujetId) {
        return candidatureRepository.findBySujetIdOrderByDateDepotAsc(sujetId)
                .stream().map(candidatureMapper::toResponse).toList();
    }

    public List<CandidatureResponse> getMesCandidatures() {
        User etudiant = userService.getCurrentUser();
        return candidatureRepository.findByEtudiantIdOrderByDateDepotDesc(etudiant.getId())
                .stream().map(candidatureMapper::toResponse).toList();
    }

    public List<AffectationResponse> getAffectationsParSujet(Long sujetId) {
        return affectationRepository.findBySujetId(sujetId)
                .stream().map(affectationMapper::toResponse).toList();
    }

    public List<LivrableResponse> getLivrablesParSujet(Long sujetId) {
        return livrableRepository.findBySujetIdOrderByDateDepotDesc(sujetId)
                .stream().map(livrableMapper::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers privés
    // ─────────────────────────────────────────────────────────────────

    private SujetProjet getSujetOrThrow(Long id) {
        return sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable : " + id));
    }

    private Candidature getCandidatureOrThrow(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable : " + id));
    }

    private Affectation getAffectationOrThrow(Long id) {
        return affectationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable : " + id));
    }
}
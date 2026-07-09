package tn.esprit.backend_esprittech.service.candidature;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.backend_esprittech.dto.candidature.request.CandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.DecisionCandidatureRequest;
import tn.esprit.backend_esprittech.dto.candidature.request.RetraitEtudiantRequest;
import tn.esprit.backend_esprittech.dto.candidature.response.AffectationResponse;
import tn.esprit.backend_esprittech.dto.candidature.response.CandidatureResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.candidature.Affectation;
import tn.esprit.backend_esprittech.entity.candidature.Candidature;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutAffectation;
import tn.esprit.backend_esprittech.enums.StatutCandidature;
import tn.esprit.backend_esprittech.mapper.candidature.AffectationMapper;
import tn.esprit.backend_esprittech.mapper.candidature.CandidatureMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.candidature.CandidatureRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.sujetprojet.SujetProjetService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * Service Candidature — couvre US-14, US-16, US-17, US-18, US-19, US-20, US-22.
 */
@Service
@RequiredArgsConstructor
public class CandidatureService {

    private final CandidatureRepository candidatureRepository;
    private final AffectationRepository affectationRepository;
    private final SujetProjetRepository sujetProjetRepository;
    private final UserRepository userRepository;
    private final CandidatureMapper candidatureMapper;
    private final AffectationMapper affectationMapper;
    private final UserService userService;
    private final SujetProjetService sujetProjetService;

    // US-14 : Ouvrir candidatures
    @Transactional
    public void ouvrirCandidatures(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        checkSujetAccess(sujet);
        if (sujet.getStatut() != StatutSujet.VALIDE) {
            throw new IllegalStateException(
                    "Seul un sujet Validé peut être ouvert aux candidatures. Statut actuel : " + sujet.getStatut());
        }
        sujet.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        sujet.setCatalogue(true);
        sujetProjetRepository.save(sujet);
    }

    // US-16 : Déposer candidature
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

    // US-17 & US-18 : Décider candidature
    @Transactional
    public CandidatureResponse deciderCandidature(Long candidatureId, DecisionCandidatureRequest request) {
        Candidature candidature = getCandidatureOrThrow(candidatureId);
        checkSujetAccess(getSujetOrThrow(candidature.getSujetId()));

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

            // Archiver autres candidatures en attente
            List<Candidature> autresEnAttente = candidatureRepository
                    .findByEtudiantIdAndStatut(candidature.getEtudiant().getId(), StatutCandidature.DEPOSEE)
                    .stream()
                    .filter(c -> !c.getId().equals(candidature.getId()))
                    .toList();

            autresEnAttente.forEach(c -> {
                c.setStatut(StatutCandidature.ARCHIVEE);
                c.setDateDecision(Instant.now());
            });
            candidatureRepository.saveAll(autresEnAttente);
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

    // US-19 + US-20 : Fermer candidatures
    @Transactional
    public void fermerCandidatures(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        checkSujetAccess(sujet);

        if (sujet.getStatut() != StatutSujet.CANDIDATURE_OUVERTE) {
            throw new IllegalStateException(
                    "Seul un sujet en CandidatureOuverte peut être fermé. Statut actuel : " + sujet.getStatut());
        }

        sujet.setStatut(StatutSujet.REALISATION_EN_COURS);

        List<Candidature> enAttente = candidatureRepository
                .findBySujetIdAndStatut(sujetId, StatutCandidature.DEPOSEE);
        enAttente.forEach(c -> c.setStatut(StatutCandidature.ARCHIVEE));
        candidatureRepository.saveAll(enAttente);

        sujetProjetRepository.save(sujet);
    }

    // US-22 : Retrait d'étudiant
    @Transactional
    public AffectationResponse retirerEtudiant(Long affectationId, RetraitEtudiantRequest request) {
        Affectation affectation = getAffectationOrThrow(affectationId);
        checkSujetAccess(getSujetOrThrow(affectation.getSujetId()));

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

    // US-24 : Déclaration de terminaison
    @Transactional
    public void declarerTerminaison(Long sujetId) {
        SujetProjet sujet = getSujetOrThrow(sujetId);
        checkSujetAccess(sujet);

        if (sujet.getStatut() != StatutSujet.REALISATION_EN_COURS) {
            throw new IllegalStateException(
                    "La terminaison ne peut être déclarée que depuis RéalisationEnCours.");
        }

        sujet.setStatut(StatutSujet.REALISATION_TERMINEE);
        sujetProjetRepository.save(sujet);
    }

    // Lectures
    @Transactional(readOnly = true)
    public List<CandidatureResponse> getCandidaturesParSujet(Long sujetId) {
        return candidatureRepository.findBySujetIdOrderByDateDepotAsc(sujetId)
                .stream().map(candidatureMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponse> getMesCandidatures() {
        User etudiant = userService.getCurrentUser();
        List<Candidature> candidatures = candidatureRepository.findByEtudiantIdOrderByDateDepotDesc(etudiant.getId());
        return mapWithSujetContext(candidatures);
    }

    /** US-16 : retrait par l'étudiant tant que la candidature n'a pas été traitée. */
    @Transactional
    public void retirerMaCandidature(Long candidatureId) {
        User etudiant = userService.getCurrentUser();
        Candidature candidature = getCandidatureOrThrow(candidatureId);

        if (!candidature.getEtudiant().getId().equals(etudiant.getId())) {
            throw new AccessDeniedException("Vous ne pouvez retirer que vos propres candidatures.");
        }
        if (candidature.getStatut() != StatutCandidature.DEPOSEE) {
            throw new IllegalStateException("Seule une candidature en attente peut être retirée.");
        }

        candidatureRepository.delete(candidature);
    }

    @Transactional(readOnly = true)
    public List<AffectationResponse> getAffectationsParSujet(Long sujetId) {
        sujetProjetService.getSujetById(sujetId);
        return affectationRepository.findBySujetIdAndStatut(sujetId, StatutAffectation.ACTIVE)
                .stream()
                .map(affectationMapper::toResponse)
                .toList();
    }

    // Helpers
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

    private List<CandidatureResponse> mapWithSujetContext(List<Candidature> candidatures) {
        if (candidatures.isEmpty()) {
            return List.of();
        }

        List<Long> sujetIds = candidatures.stream()
                .map(Candidature::getSujetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, SujetProjet> sujets = sujetProjetRepository.findAllById(sujetIds).stream()
                .collect(Collectors.toMap(SujetProjet::getId, Function.identity()));

        List<Long> encadrantIds = sujets.values().stream()
                .map(SujetProjet::getEncadrantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> encadrants = userRepository.findAllById(encadrantIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return candidatures.stream()
                .map(c -> {
                    SujetProjet sujet = sujets.get(c.getSujetId());
                    User encadrant = sujet != null ? encadrants.get(sujet.getEncadrantId()) : null;
                    return candidatureMapper.toResponse(c, sujet, encadrant);
                })
                .toList();
    }

    private void checkSujetAccess(SujetProjet sujet) {
        User current = userService.getCurrentUser();
        if (current.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        if (current.getId().equals(sujet.getEncadrantId())
                && (current.getRole() == Role.ROLE_ENSEIGNANT || current.getRole() == Role.ROLE_CHEF_EQUIPE)) {
            return;
        }
        throw new AccessDeniedException("Vous n'êtes pas autorisé à gérer ce sujet.");
    }
}
package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.InvaliderSujetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class SujetProjetService {

    private static final List<StatutSujet> STATUTS_DISPONIBLES = List.of(
            StatutSujet.VALIDE,
            StatutSujet.CANDIDATURE_OUVERTE,
            StatutSujet.CANDIDATURE_FERMEE
    );

    private final SujetProjetRepository sujetProjetRepository;
    private final SujetProjetMapper sujetProjetMapper;
    private final UserService userService;
    private final UserRepository userRepository;
    private final SujetReferenceService referenceService;
    private final EquipeRechercheRepository equipeRechercheRepository;

    public SujetProjetService(SujetProjetRepository sujetProjetRepository,
                              SujetProjetMapper sujetProjetMapper,
                              UserService userService,
                              UserRepository userRepository,
                              SujetReferenceService referenceService,
                              EquipeRechercheRepository equipeRechercheRepository) {
        this.sujetProjetRepository = sujetProjetRepository;
        this.sujetProjetMapper = sujetProjetMapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.referenceService = referenceService;
        this.equipeRechercheRepository = equipeRechercheRepository;
    }

    public SujetProjetResponse creerSujet(SujetProjetRequest request) {
        User encadrant = getCurrentEnseignant();
        SujetProjet sujet = sujetProjetMapper.toEntity(request, encadrant.getId());
        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsPourCandidatures() {
        List<StatutSujet> statuts = List.of(
                StatutSujet.VALIDE,
                StatutSujet.CANDIDATURE_OUVERTE,
                StatutSujet.CANDIDATURE_FERMEE,
                StatutSujet.REALISATION_EN_COURS,
                StatutSujet.REALISATION_TERMINEE
        );
        return toResponses(sujetProjetRepository.findByStatutInOrderByDateCreationDesc(statuts));
    }

    /**
     * US-12 / BF-03 : sujets en attente de validation.
     *
     * TEMPORAIRE : le module Équipes de recherche n'est pas encore finalisé (en cours de
     * développement par un autre membre de l'équipe). En attendant, tout CHEF_EQUIPE voit
     * et peut valider l'ensemble des sujets soumis, sans filtrage par équipe. Une fois le
     * module Équipes prêt, réintroduire le filtrage via {@link #getMembresEquipeIds}.
     */
    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsEnAttenteValidation() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ADMIN && current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new AccessDeniedException("Seul le chef d'équipe ou l'admin peut consulter cette liste.");
        }

        List<SujetProjet> enAttente = sujetProjetRepository
                .findByStatutOrderByDateCreationDesc(StatutSujet.SOUMIS_EN_VALIDATION);
        return toResponses(enAttente);
    }

    /** US-12 : validation d'un sujet par le chef d'équipe (ou l'admin). */
    public SujetProjetResponse validerSujet(Long id) {
        SujetProjet sujet = getSujetEntiteOrThrow(id);
        checkChefOuAdmin(sujet);

        if (sujet.getStatut() != StatutSujet.SOUMIS_EN_VALIDATION) {
            throw new IllegalStateException("Seul un sujet soumis en validation peut être validé.");
        }

        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setDateValidation(LocalDateTime.now());
        return toResponse(sujetProjetRepository.save(sujet));
    }

    /** US-13 : invalidation d'un sujet par le chef d'équipe (ou l'admin), motif obligatoire. */
    public SujetProjetResponse invaliderSujet(Long id, InvaliderSujetRequest request) {
        SujetProjet sujet = getSujetEntiteOrThrow(id);
        checkChefOuAdmin(sujet);

        if (sujet.getStatut() != StatutSujet.SOUMIS_EN_VALIDATION) {
            throw new IllegalStateException("Seul un sujet soumis en validation peut être invalidé.");
        }

        sujet.setStatut(StatutSujet.INVALIDE);
        sujet.setMotifInvalidation(request.motif());
        sujet.setDateValidation(LocalDateTime.now());
        return toResponse(sujetProjetRepository.save(sujet));
    }

    private SujetProjet getSujetEntiteOrThrow(Long id) {
        return sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable : " + id));
    }

    /** TEMPORAIRE : pas de filtrage par équipe tant que ce module n'est pas finalisé (voir plus haut). */
    private void checkChefOuAdmin(SujetProjet sujet) {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ADMIN && current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à valider ce sujet.");
        }
    }

    /** Identifiants de l'encadrant lui-même + des membres de son équipe (le chef peut être encadrant aussi). */
    private Set<Long> getMembresEquipeIds(User chef) {
        EquipeRecherche equipe = equipeRechercheRepository.findByChefId(chef.getId()).orElse(null);
        if (equipe == null) {
            return Set.of(chef.getId());
        }
        return Stream.concat(
                Stream.of(chef.getId()),
                equipe.getMembers().stream().map(User::getId)
        ).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getMesSujets(CategorieSujet categorie, StatutSujet statut) {
        Long encadrantId = getCurrentEnseignant().getId();
        List<SujetProjet> sujets;

        if (categorie != null && statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(encadrantId)
                    .stream()
                    .filter(s -> s.getCategorie() == categorie && s.getStatut() == statut)
                    .toList();
        } else if (categorie != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndCategorieOrderByDateCreationDesc(encadrantId, categorie);
        } else if (statut != null) {
            sujets = sujetProjetRepository.findByEncadrantIdAndStatutOrderByDateCreationDesc(encadrantId, statut);
        } else {
            sujets = sujetProjetRepository.findByEncadrantIdOrderByDateCreationDesc(encadrantId);
        }

        return toResponses(sujets);
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsDisponibles(CategorieSujet categorie) {
        // Le statut CANDIDATURE_OUVERTE est le seul filtre nécessaire.
        // Le champ catalogue est mis à true automatiquement à l'ouverture (CandidatureService).
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutInOrderByDateCreationDesc(STATUTS_DISPONIBLES)
                .stream()
                .toList();

        if (categorie != null) {
            sujets = sujets.stream().filter(s -> s.getCategorie() == categorie).toList();
        }

        return toResponses(sujets);
    }

    @Transactional(readOnly = true)
    public SujetProjetResponse getSujetById(Long id) {
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        User current = userService.getCurrentUser();
        boolean isOwner = sujet.getEncadrantId().equals(current.getId());
        boolean isPublic = sujet.getCatalogue() && STATUTS_DISPONIBLES.contains(sujet.getStatut());

        if (!isOwner && !isPublic) {
            throw new AccessDeniedException("Accès refusé à ce sujet");
        }

        return toResponse(sujet);
    }

    public SujetProjetResponse modifierSujet(Long id, SujetProjetRequest request) {
        Long encadrantId = getCurrentEnseignant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return toResponse(sujetProjetRepository.save(sujet));
    }

    public void supprimerSujet(Long id) {
        Long encadrantId = getCurrentEnseignant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetRepository.delete(sujet);
    }

    @Transactional(readOnly = true)
    public List<String> getTechnologiesDisponibles() {
        return referenceService.getAllTechnologieNoms();
    }

    @Transactional(readOnly = true)
    public List<String> getPrerequisSuggestions() {
        return referenceService.getAllPrerequisNoms();
    }

    @Transactional(readOnly = true)
    public List<String> getDomainesSuggestions() {
        return referenceService.getAllDomaineNoms();
    }

    private User getCurrentEnseignant() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new AccessDeniedException("Seuls les enseignants peuvent déposer ou gérer des sujets");
        }
        return current;
    }

    private SujetProjetResponse toResponse(SujetProjet sujet) {
        String encadrantNom = userRepository.findById(sujet.getEncadrantId())
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse("Encadrant inconnu");
        return sujetProjetMapper.toResponse(sujet, encadrantNom);
    }

    private List<SujetProjetResponse> toResponses(List<SujetProjet> sujets) {
        Map<Long, String> noms = userRepository.findAllById(
                sujets.stream().map(SujetProjet::getEncadrantId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u.getPrenom() + " " + u.getNom()));

        return sujets.stream()
                .map(s -> sujetProjetMapper.toResponse(s, noms.getOrDefault(s.getEncadrantId(), "Encadrant inconnu")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getProjetsEvaluables() {
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutOrderByDateCreationDesc(
                StatutSujet.REALISATION_TERMINEE
        );
        return toResponses(sujets);
    }
}
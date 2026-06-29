package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SujetProjetService {

    private static final List<StatutSujet> STATUTS_DISPONIBLES = List.of(
            StatutSujet.VALIDE,
            StatutSujet.CANDIDATURE_OUVERTE,
            StatutSujet.CANDIDATURE_FERMEE
    );

    private static final List<StatutSujet> STATUTS_A_VALIDER = List.of(
            StatutSujet.SOUMIS_EN_VALIDATION,
            StatutSujet.EN_ATTENTE
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
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutInOrderByDateCreationDesc(STATUTS_DISPONIBLES)
                .stream()
                .filter(SujetProjet::getCatalogue)
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
        boolean isChefValidator = canChefAccessSujet(current, sujet);

        if (!isOwner && !isPublic && !isChefValidator) {
            throw new AccessDeniedException("Accès refusé à ce sujet");
        }

        return toResponse(sujet);
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsAValider(CategorieSujet categorie, StatutSujet statut) {
        getCurrentChefEquipe();
        List<StatutSujet> statuts = statut != null ? List.of(statut) : STATUTS_A_VALIDER;

        List<SujetProjet> sujets = sujetProjetRepository.findByStatutInOrderByDateCreationDesc(statuts);

        if (categorie != null) {
            sujets = sujets.stream().filter(s -> s.getCategorie() == categorie).toList();
        }

        return toResponses(sujets);
    }

    public SujetProjetResponse validerSujet(Long id) {
        User chef = getCurrentChefEquipe();
        SujetProjet sujet = findSujetForValidation(chef, id);

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        sujet.setCatalogue(true);
        sujet.setDateValidation(LocalDateTime.now());
        sujet.setMotifInvalidation(null);

        return toResponse(sujetProjetRepository.save(sujet));
    }

    public SujetProjetResponse invaliderSujet(Long id, SujetInvalidationRequest request) {
        User chef = getCurrentChefEquipe();
        SujetProjet sujet = findSujetForValidation(chef, id);

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.INVALIDE);
        sujet.setCatalogue(false);
        sujet.setMotifInvalidation(request.motif().trim());
        sujet.setDateValidation(LocalDateTime.now());

        return toResponse(sujetProjetRepository.save(sujet));
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
        User encadrant = userRepository.findById(sujet.getEncadrantId()).orElse(null);
        return sujetProjetMapper.toResponse(
                sujet,
                formatNom(encadrant),
                encadrant != null ? encadrant.getEmail() : null,
                getEquipeNom(encadrant)
        );
    }

    private List<SujetProjetResponse> toResponses(List<SujetProjet> sujets) {
        Map<Long, User> encadrants = userRepository.findAllById(
                sujets.stream().map(SujetProjet::getEncadrantId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return sujets.stream()
                .map(s -> {
                    User encadrant = encadrants.get(s.getEncadrantId());
                    return sujetProjetMapper.toResponse(
                            s,
                            formatNom(encadrant),
                            encadrant != null ? encadrant.getEmail() : null,
                            getEquipeNom(encadrant)
                    );
                })
                .toList();
    }

    private User getCurrentChefEquipe() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new AccessDeniedException("Seuls les chefs d'équipe peuvent valider des sujets");
        }
        return current;
    }

    private User getCurrentAdmin() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Accès réservé à l'administrateur");
        }
        return current;
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SujetProjet findSujetForValidation(User chef, Long id) {
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        if (!canChefValidateSujet(chef, sujet)) {
            throw new IllegalArgumentException("Sujet introuvable ou hors de votre équipe");
        }

        return sujet;
    }

    private boolean canChefAccessSujet(User user, SujetProjet sujet) {
        if (user.getRole() != Role.ROLE_CHEF_EQUIPE) {
            return false;
        }
        if (STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            return canChefValidateSujet(user, sujet);
        }
        return equipeRechercheRepository.findByChefId(user.getId())
                .map(equipe -> isEncadrantVisibleForChef(user.getId(), equipe.getId(), sujet.getEncadrantId()))
                .orElse(false);
    }

    /**
     * Sans équipe : toutes les demandes en attente.
     * Avec équipe : sujets du chef, des membres de l'équipe, ou des enseignants non encore affectés.
     */
    private List<SujetProjet> filterByChefEquipe(User chef, List<SujetProjet> sujets) {
        Optional<EquipeRecherche> equipeOpt = equipeRechercheRepository.findByChefId(chef.getId());
        if (equipeOpt.isEmpty()) {
            return sujets;
        }

        Long equipeId = equipeOpt.get().getId();
        return sujets.stream()
                .filter(s -> isEncadrantVisibleForChef(chef.getId(), equipeId, s.getEncadrantId()))
                .toList();
    }

    private boolean isEncadrantVisibleForChef(Long chefId, Long equipeId, Long encadrantId) {
        if (encadrantId.equals(chefId)) {
            return true;
        }
        return userRepository.findById(encadrantId)
                .map(encadrant -> {
                    EquipeRecherche encadrantEquipe = encadrant.getEquipeRecherche();
                    return encadrantEquipe == null || encadrantEquipe.getId().equals(equipeId);
                })
                .orElse(false);
    }

    private boolean canChefValidateSujet(User chef, SujetProjet sujet) {
        return filterByChefEquipe(chef, List.of(sujet)).size() == 1;
    }

    private String formatNom(User user) {
        if (user == null) {
            return "Encadrant inconnu";
        }
        return user.getPrenom() + " " + user.getNom();
    }

    private String getEquipeNom(User encadrant) {
        if (encadrant == null || encadrant.getEquipeRecherche() == null) {
            return null;
        }
        return encadrant.getEquipeRecherche().getNom();
    }

    @Transactional(readOnly = true)
    public Page<SujetProjetResponse> listSujetsForAdmin(CategorieSujet categorie,
                                                        StatutSujet statut,
                                                        String search,
                                                        Pageable pageable) {
        getCurrentAdmin();
        Page<SujetProjet> page = sujetProjetRepository.searchForAdmin(categorie, statut, normalizeSearch(search), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SujetProjetResponse> listDemandesForAdmin(CategorieSujet categorie,
                                                          String search,
                                                          Pageable pageable) {
        getCurrentAdmin();
        Page<SujetProjet> page = sujetProjetRepository.searchDemandesForAdmin(
                STATUTS_A_VALIDER, categorie, normalizeSearch(search), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<SujetProjetResponse> listSujetsDisponiblesForAdmin(CategorieSujet categorie,
                                                                   String search,
                                                                   Pageable pageable) {
        getCurrentAdmin();
        Page<SujetProjet> page = sujetProjetRepository.searchDisponiblesForAdmin(
                STATUTS_DISPONIBLES, categorie, normalizeSearch(search), pageable);
        return page.map(this::toResponse);
    }

    public SujetProjetResponse validerSujetAsAdmin(Long id) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.CANDIDATURE_OUVERTE);
        sujet.setCatalogue(true);
        sujet.setDateValidation(LocalDateTime.now());
        sujet.setMotifInvalidation(null);

        return toResponse(sujetProjetRepository.save(sujet));
    }

    public SujetProjetResponse invaliderSujetAsAdmin(Long id, SujetInvalidationRequest request) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.INVALIDE);
        sujet.setCatalogue(false);
        sujet.setMotifInvalidation(request.motif().trim());
        sujet.setDateValidation(LocalDateTime.now());

        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public SujetProjetResponse getSujetForAdmin(Long id) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        return toResponse(sujet);
    }

    public SujetProjetResponse modifierSujetAsAdmin(Long id, SujetProjetRequest request) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return toResponse(sujetProjetRepository.save(sujet));
    }

    public void supprimerSujetAsAdmin(Long id) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetRepository.delete(sujet);
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getProjetsEvaluables() {
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutOrderByDateCreationDesc(
                StatutSujet.REALISATION_TERMINEE
        );
        return toResponses(sujets);
    }
}

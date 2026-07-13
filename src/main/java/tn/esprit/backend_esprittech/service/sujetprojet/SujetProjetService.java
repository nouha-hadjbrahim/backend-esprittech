package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.AdminCreateSujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetInvalidationRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.SujetProjetRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.SujetProjetResponse;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.CategorieSujet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutAffectation;
import tn.esprit.backend_esprittech.mapper.sujetprojet.SujetProjetMapper;
import tn.esprit.backend_esprittech.repository.UserRepository;
import tn.esprit.backend_esprittech.repository.candidature.AffectationRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.TerminaisonSujetRequest;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.projetcatalogue.ProjetCatalogueService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SujetProjetService {

    private static final List<StatutSujet> STATUTS_DISPONIBLES = List.of(
            StatutSujet.VALIDE,
            StatutSujet.CANDIDATURE_OUVERTE
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
    private final AffectationRepository affectationRepository;
    private final ProjetCatalogueService projetCatalogueService;

    public SujetProjetService(SujetProjetRepository sujetProjetRepository,
                              SujetProjetMapper sujetProjetMapper,
                              UserService userService,
                              UserRepository userRepository,
                              SujetReferenceService referenceService,
                              EquipeRechercheRepository equipeRechercheRepository,
                              AffectationRepository affectationRepository,
                              ProjetCatalogueService projetCatalogueService) {
        this.sujetProjetRepository = sujetProjetRepository;
        this.sujetProjetMapper = sujetProjetMapper;
        this.userService = userService;
        this.userRepository = userRepository;
        this.referenceService = referenceService;
        this.equipeRechercheRepository = equipeRechercheRepository;
        this.affectationRepository = affectationRepository;
        this.projetCatalogueService = projetCatalogueService;
    }

    @Historise(
            action = ActionType.CREATE,
            entityType = EntityType.SUJET_PROJET,
            entityIdExpression = "#result.id()",
            newValuesExpression = "{'statut': #result.statut()}",
            summaryExpression = "'Création sujet ' + #result.titre()")
    public SujetProjetResponse creerSujet(SujetProjetRequest request) {
        User encadrant = getCurrentEncadrant();
        ensureEncadrantAffilie(encadrant);
        SujetProjet sujet = sujetProjetMapper.toEntity(request, encadrant.getId());
        if (encadrant.getRole() == Role.ROLE_CHEF_EQUIPE) {
            LocalDateTime now = LocalDateTime.now();
            sujet.setStatut(StatutSujet.VALIDE);
            sujet.setCatalogue(true);
            sujet.setDateSoumission(now);
            sujet.setDateValidation(now);
            sujet.setMotifInvalidation(null);
        }
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
     * L'admin voit toutes les demandes ; chaque chef d'équipe ne voit que celles
     * des membres affiliés à son équipe.
     */
    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsEnAttenteValidation() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ADMIN && current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new AccessDeniedException("Seul le chef d'équipe ou l'admin peut consulter cette liste.");
        }

        List<SujetProjet> enAttente = sujetProjetRepository
                .findByStatutOrderByDateCreationDesc(StatutSujet.SOUMIS_EN_VALIDATION);

        if (current.getRole() == Role.ROLE_CHEF_EQUIPE) {
            enAttente = filterByChefEquipe(current, enAttente);
        }

        return toResponses(enAttente);
    }
/* 
    public SujetProjetResponse validerSujet(Long id) {
        SujetProjet sujet = getSujetEntiteOrThrow(id);
        checkChefOuAdmin(sujet);

        if (sujet.getStatut() != StatutSujet.SOUMIS_EN_VALIDATION) {
            throw new IllegalStateException("Seul un sujet soumis en validation peut être validé.");
        }

        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setDateValidation(LocalDateTime.now());
        return toResponse(sujetProjetRepository.save(sujet));
    } */

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

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getMesSujets(CategorieSujet categorie, StatutSujet statut) {
        Long encadrantId = getCurrentEncadrant().getId();
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
        List<SujetProjet> sujets = sujetProjetRepository
                .findByCatalogueTrueAndStatutInOrderByDateCreationDesc(STATUTS_DISPONIBLES);

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
        User chef = getCurrentChefEquipe();
        List<StatutSujet> statuts = statut != null ? List.of(statut) : STATUTS_A_VALIDER;

        List<SujetProjet> sujets = sujetProjetRepository.findByStatutInOrderByDateCreationDesc(statuts);
        sujets = filterByChefEquipe(chef, sujets);

        if (categorie != null) {
            sujets = sujets.stream().filter(s -> s.getCategorie() == categorie).toList();
        }

        return toResponses(sujets);
    }

    /**
     * Sujets déposés par les enseignants affiliés à l'équipe du chef connecté.
     */
    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getSujetsEquipe(CategorieSujet categorie, StatutSujet statut) {
        User chef = getCurrentChefEquipe();
        Optional<EquipeRecherche> equipeOpt = resolveEquipeForChef(chef);
        if (equipeOpt.isEmpty()) {
            return List.of();
        }

        Set<Long> encadrantIds = getVisibleEncadrantIds(chef.getId(), equipeOpt.get().getId());
        if (encadrantIds.isEmpty()) {
            return List.of();
        }

        List<StatutSujet> statuts = statut != null
                ? List.of(statut)
                : List.of(StatutSujet.values());

        List<SujetProjet> sujets = sujetProjetRepository
                .findByEncadrantIdInAndStatutInOrderByDateCreationDesc(
                        encadrantIds.stream().toList(), statuts);

        if (categorie != null) {
            sujets = sujets.stream().filter(s -> s.getCategorie() == categorie).toList();
        }

        return toResponses(sujets);
    }

    public String getTitreById(Long id) {
        return sujetProjetRepository.findById(id)
                .map(SujetProjet::getTitre)
                .orElse("Inconnu");
    }

    public Map<String, Object> getOldValues(Long id) {
        SujetProjet s = sujetProjetRepository.findById(id).orElse(null);
        if (s == null) return Map.of();
        Map<String, Object> map = new HashMap<>();
        map.put("statut", s.getStatut() != null ? s.getStatut().name() : null);
        map.put("titre", s.getTitre());
        map.put("catalogue", s.getCatalogue());
        return map;
    }

    @Historise(action = ActionType.VALIDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", newValuesExpression = "{'statut': 'VALIDE'}", summaryExpression = "'Validation sujet ' + @sujetProjetService.getTitreById(#id)")
    public SujetProjetResponse validerSujet(Long id) {
        User chef = getCurrentChefEquipe();
        SujetProjet sujet = findSujetForValidation(chef, id);

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setCatalogue(true);
        sujet.setDateValidation(LocalDateTime.now());
        sujet.setMotifInvalidation(null);

        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Historise(action = ActionType.INVALIDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", newValuesExpression = "{'statut': 'INVALIDE'}", metadataExpression = "{'motif': #request.motif()}", summaryExpression = "'Invalidation sujet ' + @sujetProjetService.getTitreById(#id)")
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

    @Historise(action = ActionType.UPDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", summaryExpression = "'Modification sujet ' + @sujetProjetService.getTitreById(#id)")
    public SujetProjetResponse modifierSujet(Long id, SujetProjetRequest request) {
        Long encadrantId = getCurrentEncadrant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", summaryExpression = "'Suppression sujet ' + @sujetProjetService.getTitreById(#id)")
    public void supprimerSujet(Long id) {
        Long encadrantId = getCurrentEncadrant().getId();
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

    private User getCurrentEncadrant() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ENSEIGNANT && current.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new AccessDeniedException("Seuls les enseignants et chefs d'équipe peuvent déposer ou gérer des sujets");
        }
        return current;
    }

    private User getCurrentEnseignant() {
        User current = userService.getCurrentUser();
        if (current.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new AccessDeniedException("Seuls les enseignants peuvent déposer ou gérer des sujets");
        }
        return current;
    }

    private void ensureEncadrantAffilie(User encadrant) {
        User managed = userRepository.findById(encadrant.getId()).orElse(encadrant);
        if (managed.getEquipeRecherche() != null) {
            return;
        }
        if (managed.getRole() == Role.ROLE_CHEF_EQUIPE && resolveEquipeForChef(managed).isPresent()) {
            return;
        }
        throw new IllegalArgumentException(
                "Vous devez être affilié à une équipe de recherche pour déposer un sujet.");
    }

    private SujetProjetResponse toResponse(SujetProjet sujet) {
        User encadrant = userRepository.findById(sujet.getEncadrantId()).orElse(null);
        return sujetProjetMapper.toResponse(
                sujet,
                formatNom(encadrant),
                encadrant != null ? encadrant.getEmail() : null,
                getEquipeNom(encadrant),
                countMembresActifs(sujet.getId())
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
                            getEquipeNom(encadrant),
                            countMembresActifs(s.getId())
                    );
                })
                .toList();
    }

    private int countMembresActifs(Long sujetId) {
        if (sujetId == null) {
            return 0;
        }
        return (int) affectationRepository.countBySujetIdAndStatut(sujetId, StatutAffectation.ACTIVE);
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
        return resolveEquipeForChef(user)
                .map(equipe -> isEncadrantVisibleForChef(user.getId(), equipe.getId(), sujet.getEncadrantId()))
                .orElse(false);
    }

    /**
     * Chaque chef ne voit que les demandes des membres de l'équipe qu'il dirige
     * (y compris ses propres sujets s'il est aussi encadrant).
     */
    private List<SujetProjet> filterByChefEquipe(User chef, List<SujetProjet> sujets) {
        Optional<EquipeRecherche> equipeOpt = resolveEquipeForChef(chef);
        if (equipeOpt.isEmpty()) {
            return List.of();
        }

        Long equipeId = equipeOpt.get().getId();
        Set<Long> visibleEncadrantIds = getVisibleEncadrantIds(chef.getId(), equipeId);
        return sujets.stream()
                .filter(s -> visibleEncadrantIds.contains(s.getEncadrantId()))
                .toList();
    }

    /**
     * Équipe dirigée par le chef : priorité à {@code equipe_recherche.chef_id},
     * repli sur {@code user.equipe_id} (aligné avec /auth/me).
     */
    private Optional<EquipeRecherche> resolveEquipeForChef(User chef) {
        Optional<EquipeRecherche> equipeDirigee = equipeRechercheRepository.findByChefId(chef.getId());
        if (equipeDirigee.isPresent()) {
            return equipeDirigee;
        }

        return userRepository.findById(chef.getId())
                .map(User::getEquipeRecherche)
                .filter(Objects::nonNull);
    }

    private Set<Long> getVisibleEncadrantIds(Long chefId, Long equipeId) {
        Set<Long> ids = userRepository.findByEquipeRechercheId(equipeId).stream()
                .map(User::getId)
                .collect(Collectors.toCollection(HashSet::new));
        ids.add(chefId);
        return ids;
    }

    private boolean isEncadrantVisibleForChef(Long chefId, Long equipeId, Long encadrantId) {
        return getVisibleEncadrantIds(chefId, equipeId).contains(encadrantId);
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

    @Historise(action = ActionType.VALIDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", newValuesExpression = "{'statut': 'VALIDE'}", summaryExpression = "'Validation sujet par admin ' + @sujetProjetService.getTitreById(#id)")
    public SujetProjetResponse validerSujetAsAdmin(Long id) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        if (!STATUTS_A_VALIDER.contains(sujet.getStatut())) {
            throw new IllegalArgumentException("Ce sujet n'est pas en attente de validation");
        }

        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setCatalogue(true);
        sujet.setDateValidation(LocalDateTime.now());
        sujet.setMotifInvalidation(null);

        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Historise(action = ActionType.INVALIDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", newValuesExpression = "{'statut': 'INVALIDE'}", metadataExpression = "{'motif': #request.motif()}", summaryExpression = "'Invalidation sujet par admin ' + @sujetProjetService.getTitreById(#id)")
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

    @Historise(action = ActionType.UPDATE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", summaryExpression = "'Modification sujet par admin ' + @sujetProjetService.getTitreById(#id)")
    public SujetProjetResponse modifierSujetAsAdmin(Long id, SujetProjetRequest request) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetMapper.updateEntity(sujet, request);
        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", summaryExpression = "'Suppression sujet par admin ' + @sujetProjetService.getTitreById(#id)")
    public void supprimerSujetAsAdmin(Long id) {
        getCurrentAdmin();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));
        sujetProjetRepository.delete(sujet);
    }

    public SujetProjetResponse creerSujetAsAdmin(AdminCreateSujetProjetRequest request) {
        getCurrentAdmin();
        User encadrant = userRepository.findById(request.encadrantId())
                .orElseThrow(() -> new IllegalArgumentException("Encadrant introuvable"));

        if (encadrant.getRole() != Role.ROLE_ENSEIGNANT && encadrant.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new IllegalArgumentException("L'encadrant doit être enseignant ou chef d'équipe.");
        }

        ensureEncadrantAffilie(encadrant);

        SujetProjet sujet = sujetProjetMapper.toEntity(request.sujet(), encadrant.getId());
        LocalDateTime now = LocalDateTime.now();
        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setCatalogue(true);
        sujet.setDateSoumission(now);
        sujet.setDateValidation(now);
        sujet.setMotifInvalidation(null);

        return toResponse(sujetProjetRepository.save(sujet));
    }

    @Transactional(readOnly = true)
    public List<SujetProjetResponse> getProjetsEvaluables() {
        if (userService.getCurrentUser().getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Acces admin requis");
        }
        List<SujetProjet> sujets = sujetProjetRepository.findByStatutOrderByDateCreationDesc(
                StatutSujet.REALISATION_TERMINEE
        );
        return toResponses(sujets);
    }

    @Transactional
    @Historise(action = ActionType.DECLARER_TERMINAISON, entityType = EntityType.SUJET_PROJET, entityIdExpression = "#id", oldValuesExpression = "@sujetProjetService.getOldValues(#id)", newValuesExpression = "{'statut': 'REALISATION_TERMINEE'}", summaryExpression = "'Déclaration terminaison sujet ' + @sujetProjetService.getTitreById(#id)")
    public SujetProjetResponse declarerTerminaison(Long id) {
        Long encadrantId = getCurrentEncadrant().getId();
        SujetProjet sujet = sujetProjetRepository.findById(id)
                .filter(s -> s.getEncadrantId().equals(encadrantId))
                .orElseThrow(() -> new IllegalArgumentException("Sujet introuvable"));

        if (sujet.getStatut() != StatutSujet.REALISATION_EN_COURS) {
            throw new IllegalStateException("Impossible de déclarer la terminaison : le sujet n'est pas en cours de réalisation.");
        }

        sujet.setStatut(StatutSujet.REALISATION_TERMINEE);
        sujet.setDateTerminaison(LocalDateTime.now());
        sujet.setCatalogue(true);

        SujetProjet saved = sujetProjetRepository.save(sujet);

        String coverBase64 = request != null ? request.coverImageBase64() : null;
        String coverType = request != null ? request.coverImageContentType() : null;
        projetCatalogueService.publierDepuisSujetTermine(saved, coverBase64, coverType);

        // Le score est calculé après commit par le listener d'évaluation existant,
        // puis synchronisé sur le projet catalogue (évite le rollback-only).
        return toResponse(saved);
    }

    @Transactional
    public SujetProjetResponse declarerTerminaison(Long id) {
        return declarerTerminaison(id, null);
    }
}

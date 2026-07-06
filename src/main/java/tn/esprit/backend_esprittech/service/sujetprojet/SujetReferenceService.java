package tn.esprit.backend_esprittech.service.sujetprojet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.ReferenceItemRequest;
import tn.esprit.backend_esprittech.dto.sujetprojet.response.ReferenceItemResponse;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SujetReferenceService {

    private final SujetDomaineRepository domaineRepository;
    private final SujetPrerequisRepository prerequisRepository;
    private final SujetTechnologieRepository technologieRepository;

    public SujetReferenceService(SujetDomaineRepository domaineRepository,
                                 SujetPrerequisRepository prerequisRepository,
                                 SujetTechnologieRepository technologieRepository) {
        this.domaineRepository = domaineRepository;
        this.prerequisRepository = prerequisRepository;
        this.technologieRepository = technologieRepository;
    }

    // ── Domaines ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReferenceItemResponse> getDomaines(int page, int size, String search) {
        return PagedResponse.from(searchDomaines(page, size, search), this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countDomaines() {
        return domaineRepository.count();
    }

    @Transactional(readOnly = true)
    public List<String> getAllDomaineNoms() {
        return domaineRepository.findAllByOrderByNomAsc().stream().map(SujetDomaine::getNom).toList();
    }

    @Historise(action = ActionType.CREATE, entityType = EntityType.SUJET_DOMAINE, entityIdExpression = "#result.id()", summaryExpression = "'Création du domaine ' + #result.nom()")
    public ReferenceItemResponse createDomaine(ReferenceItemRequest request) {
        String nom = normalizeNom(request.nom());
        if (domaineRepository.existsByNomIgnoreCase(nom)) {
            throw new IllegalArgumentException("Ce domaine existe déjà");
        }
        return toResponse(domaineRepository.save(SujetDomaine.builder().nom(nom).build()));
    }

    public Map<String, Object> getDomaineOldValues(Long id) {
        SujetDomaine d = findDomaine(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", d.getNom());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.SUJET_DOMAINE, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getDomaineOldValues(#id)", summaryExpression = "'Modification du domaine ' + #result.nom()")
    public ReferenceItemResponse updateDomaine(Long id, ReferenceItemRequest request) {
        SujetDomaine domaine = findDomaine(id);
        String nom = normalizeNom(request.nom());
        domaineRepository.findByNomIgnoreCase(nom)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Ce domaine existe déjà");
                });
        domaine.setNom(nom);
        return toResponse(domaineRepository.save(domaine));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.SUJET_DOMAINE, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getDomaineOldValues(#id)", summaryExpression = "'Suppression du domaine (id=' + #id + ')'")
    public void deleteDomaine(Long id) {
        domaineRepository.delete(findDomaine(id));
    }

    public ReferenceItemResponse findOrCreateDomaine(String nom) {
        String normalized = normalizeNom(nom);
        return domaineRepository.findByNomIgnoreCase(normalized)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(domaineRepository.save(SujetDomaine.builder().nom(normalized).build())));
    }

    // ── Prérequis ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReferenceItemResponse> getPrerequis(int page, int size, String search) {
        return PagedResponse.from(searchPrerequis(page, size, search), this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countPrerequis() {
        return prerequisRepository.count();
    }

    @Transactional(readOnly = true)
    public List<String> getAllPrerequisNoms() {
        return prerequisRepository.findAllByOrderByNomAsc().stream().map(SujetPrerequis::getNom).toList();
    }

    @Historise(action = ActionType.CREATE, entityType = EntityType.SUJET_PREREQUIS, entityIdExpression = "#result.id()", summaryExpression = "'Création du prérequis ' + #result.nom()")
    public ReferenceItemResponse createPrerequis(ReferenceItemRequest request) {
        String nom = normalizeNom(request.nom());
        if (prerequisRepository.existsByNomIgnoreCase(nom)) {
            throw new IllegalArgumentException("Ce prérequis existe déjà");
        }
        return toResponse(prerequisRepository.save(SujetPrerequis.builder().nom(nom).build()));
    }

    public Map<String, Object> getPrerequisOldValues(Long id) {
        SujetPrerequis p = findPrerequis(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", p.getNom());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.SUJET_PREREQUIS, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getPrerequisOldValues(#id)", summaryExpression = "'Modification du prérequis ' + #result.nom()")
    public ReferenceItemResponse updatePrerequis(Long id, ReferenceItemRequest request) {
        SujetPrerequis prerequis = findPrerequis(id);
        String nom = normalizeNom(request.nom());
        prerequisRepository.findByNomIgnoreCase(nom)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Ce prérequis existe déjà");
                });
        prerequis.setNom(nom);
        return toResponse(prerequisRepository.save(prerequis));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.SUJET_PREREQUIS, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getPrerequisOldValues(#id)", summaryExpression = "'Suppression du prérequis (id=' + #id + ')'")
    public void deletePrerequis(Long id) {
        prerequisRepository.delete(findPrerequis(id));
    }

    public ReferenceItemResponse findOrCreatePrerequis(String nom) {
        String normalized = normalizeNom(nom);
        return prerequisRepository.findByNomIgnoreCase(normalized)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(prerequisRepository.save(SujetPrerequis.builder().nom(normalized).build())));
    }

    // ── Technologies ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReferenceItemResponse> getTechnologies(int page, int size, String search) {
        return PagedResponse.from(searchTechnologies(page, size, search), this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countTechnologies() {
        return technologieRepository.count();
    }

    @Transactional(readOnly = true)
    public List<String> getAllTechnologieNoms() {
        return technologieRepository.findAllByOrderByNomAsc().stream().map(SujetTechnologie::getNom).toList();
    }

    @Historise(action = ActionType.CREATE, entityType = EntityType.SUJET_TECHNOLOGIE, entityIdExpression = "#result.id()", summaryExpression = "'Création de la technologie ' + #result.nom()")
    public ReferenceItemResponse createTechnologie(ReferenceItemRequest request) {
        String nom = normalizeNom(request.nom());
        if (technologieRepository.existsByNomIgnoreCase(nom)) {
            throw new IllegalArgumentException("Cette technologie existe déjà");
        }
        return toResponse(technologieRepository.save(SujetTechnologie.builder().nom(nom).build()));
    }

    public Map<String, Object> getTechnologieOldValues(Long id) {
        SujetTechnologie t = findTechnologie(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", t.getNom());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.SUJET_TECHNOLOGIE, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getTechnologieOldValues(#id)", summaryExpression = "'Modification de la technologie ' + #result.nom()")
    public ReferenceItemResponse updateTechnologie(Long id, ReferenceItemRequest request) {
        SujetTechnologie technologie = findTechnologie(id);
        String nom = normalizeNom(request.nom());
        technologieRepository.findByNomIgnoreCase(nom)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(t -> {
                    throw new IllegalArgumentException("Cette technologie existe déjà");
                });
        technologie.setNom(nom);
        return toResponse(technologieRepository.save(technologie));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.SUJET_TECHNOLOGIE, entityIdExpression = "#id", oldValuesExpression = "@sujetReferenceService.getTechnologieOldValues(#id)", summaryExpression = "'Suppression de la technologie (id=' + #id + ')'")
    public void deleteTechnologie(Long id) {
        technologieRepository.delete(findTechnologie(id));
    }

    public ReferenceItemResponse findOrCreateTechnologie(String nom) {
        String normalized = normalizeNom(nom);
        return technologieRepository.findByNomIgnoreCase(normalized)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(technologieRepository.save(SujetTechnologie.builder().nom(normalized).build())));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Page<SujetDomaine> searchDomaines(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        if (StringUtils.hasText(search)) {
            return domaineRepository.findByNomContainingIgnoreCase(search.trim(), pageable);
        }
        return domaineRepository.findAllByOrderByNomAsc(pageable);
    }

    private Page<SujetPrerequis> searchPrerequis(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        if (StringUtils.hasText(search)) {
            return prerequisRepository.findByNomContainingIgnoreCase(search.trim(), pageable);
        }
        return prerequisRepository.findAllByOrderByNomAsc(pageable);
    }

    private Page<SujetTechnologie> searchTechnologies(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        if (StringUtils.hasText(search)) {
            return technologieRepository.findByNomContainingIgnoreCase(search.trim(), pageable);
        }
        return technologieRepository.findAllByOrderByNomAsc(pageable);
    }

    private SujetDomaine findDomaine(Long id) {
        return domaineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine introuvable"));
    }

    private SujetPrerequis findPrerequis(Long id) {
        return prerequisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prérequis introuvable"));
    }

    private SujetTechnologie findTechnologie(Long id) {
        return technologieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technologie introuvable"));
    }

    private String normalizeNom(String nom) {
        if (!StringUtils.hasText(nom)) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        return nom.trim();
    }

    private ReferenceItemResponse toResponse(SujetDomaine domaine) {
        return new ReferenceItemResponse(domaine.getId(), domaine.getNom(), domaine.getDateCreation());
    }

    private ReferenceItemResponse toResponse(SujetPrerequis prerequis) {
        return new ReferenceItemResponse(prerequis.getId(), prerequis.getNom(), prerequis.getDateCreation());
    }

    private ReferenceItemResponse toResponse(SujetTechnologie technologie) {
        return new ReferenceItemResponse(technologie.getId(), technologie.getNom(), technologie.getDateCreation());
    }
}

package tn.esprit.backend_esprittech.service.equipe;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.equipe.EquipeDomaineRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EquipeDomaineService {

    private final EquipeDomaineRepository repository;

    public EquipeDomaineService(EquipeDomaineRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EquipeDomaineResponse> getPage(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
        Page<EquipeDomaine> result;
        if (StringUtils.hasText(search)) {
            result = repository.findByNomContainingIgnoreCase(search.trim(), pageable);
        } else {
            result = repository.findAllByOrderByNomAsc(pageable);
        }
        return PagedResponse.from(result, this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EquipeDomaineResponse> getAll() {
        return repository.findAllByOrderByNomAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    @Historise(action = ActionType.CREATE, entityType = EntityType.EQUIPE_DOMAINE, entityIdExpression = "#result.id()", summaryExpression = "'Création du domaine ' + #result.nom()")
    public EquipeDomaineResponse create(EquipeDomaineRequest request) {
        String nom = normalizeNom(request.nom());
        if (repository.existsByNomIgnoreCase(nom)) {
            throw new IllegalArgumentException("Ce domaine existe déjà");
        }
        EquipeDomaine entity = EquipeDomaine.builder().nom(nom).build();
        return toResponse(repository.save(entity));
    }

    public Map<String, Object> getOldValues(Long id) {
        EquipeDomaine e = findById(id);
        Map<String, Object> map = new HashMap<>();
        map.put("nom", e.getNom());
        return map;
    }

    @Historise(action = ActionType.UPDATE, entityType = EntityType.EQUIPE_DOMAINE, entityIdExpression = "#id", oldValuesExpression = "@equipeDomaineService.getOldValues(#id)", summaryExpression = "'Modification du domaine ' + #result.nom()")
    public EquipeDomaineResponse update(Long id, EquipeDomaineRequest request) {
        EquipeDomaine entity = findById(id);
        String nom = normalizeNom(request.nom());
        repository.findByNomIgnoreCase(nom)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Ce domaine existe déjà");
                });
        entity.setNom(nom);
        return toResponse(repository.save(entity));
    }

    @Historise(action = ActionType.DELETE, entityType = EntityType.EQUIPE_DOMAINE, entityIdExpression = "#id", oldValuesExpression = "@equipeDomaineService.getOldValues(#id)", summaryExpression = "'Suppression du domaine (id=' + #id + ')'")
    public void delete(Long id) {
        EquipeDomaine entity = findById(id);
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public EquipeDomaine getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine d'équipe introuvable (id=" + id + ")"));
    }

    private EquipeDomaine findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domaine d'équipe introuvable"));
    }

    private String normalizeNom(String nom) {
        if (!StringUtils.hasText(nom)) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        return nom.trim();
    }

    public EquipeDomaineResponse toResponse(EquipeDomaine entity) {
        return new EquipeDomaineResponse(entity.getId(), entity.getNom(), entity.getDateCreation());
    }
}

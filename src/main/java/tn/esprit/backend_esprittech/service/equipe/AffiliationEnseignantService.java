package tn.esprit.backend_esprittech.service.equipe;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.dto.equipe.AffiliationEnseignantResponse;
import tn.esprit.backend_esprittech.dto.response.UserResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.equipe.AffiliationEnseignant;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.equipe.StatutAffiliation;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.UserMapper;
import tn.esprit.backend_esprittech.repository.equipe.AffiliationEnseignantRepository;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AffiliationEnseignantService {

    private final AffiliationEnseignantRepository affiliationRepository;
    private final EquipeRechercheRepository equipeRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AffiliationEnseignantService(AffiliationEnseignantRepository affiliationRepository,
                                        EquipeRechercheRepository equipeRepository,
                                        UserRepository userRepository,
                                        UserMapper userMapper) {
        this.affiliationRepository = affiliationRepository;
        this.equipeRepository = equipeRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Transactional
    @Historise(action = ActionType.DEMANDER_AFFILIATION, entityType = EntityType.AFFILIATION_ENSEIGNANT, entityIdExpression = "#result.id()", summaryExpression = "'Demande d''affiliation à l''équipe ' + #result.equipeNom()", metadataExpression = "{'equipeId': #equipeId, 'enseignantId': #enseignant.id}")
    public AffiliationEnseignantResponse demander(Long equipeId, User enseignant) {
        if (enseignant.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new BusinessException("Seuls les enseignants peuvent demander une affiliation");
        }

        EquipeRecherche equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe introuvable (id=" + equipeId + ")"));

        if (enseignant.getEquipeRecherche() != null
                && enseignant.getEquipeRecherche().getId().equals(equipeId)) {
            throw new BusinessException("Vous êtes déjà membre de cette équipe");
        }

        if (affiliationRepository.existsByEnseignantIdAndEquipeIdAndStatut(
                enseignant.getId(), equipeId, StatutAffiliation.EN_ATTENTE)) {
            throw new BusinessException("Une demande en attente existe déjà pour cette équipe");
        }

        AffiliationEnseignant affiliation = AffiliationEnseignant.builder()
                .enseignant(enseignant)
                .equipe(equipe)
                .statut(StatutAffiliation.EN_ATTENTE)
                .build();

        return toResponse(affiliationRepository.save(affiliation));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOldValues(Long affiliationId) {
        AffiliationEnseignant a = affiliationRepository.findById(affiliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'affiliation introuvable (id=" + affiliationId + ")"));
        Map<String, Object> map = new HashMap<>();
        map.put("statut", a.getStatut().name());
        map.put("enseignantId", a.getEnseignant().getId());
        map.put("equipeId", a.getEquipe().getId());
        map.put("motifDecision", a.getMotifDecision());
        return map;
    }

    @Transactional
    @Historise(action = ActionType.TRAITER_AFFILIATION, entityType = EntityType.AFFILIATION_ENSEIGNANT, entityIdExpression = "#affiliationId", oldValuesExpression = "@affiliationEnseignantService.getOldValues(#affiliationId)", summaryExpression = "'Traitement de la demande d''affiliation (id=' + #affiliationId + ', statut=' + #result.statut() + ')'", metadataExpression = "{'equipeId': #equipeId, 'statut': #statut.name(), 'motif': #motifDecision, 'deciderId': #decider.id}")
    public AffiliationEnseignantResponse traiter(Long affiliationId, Long equipeId, User decider,
                                                  StatutAffiliation statut, String motifDecision) {
        if (decider.getRole() != Role.ROLE_ADMIN && decider.getRole() != Role.ROLE_CHEF_EQUIPE) {
            throw new BusinessException("Seuls l'admin ou le chef d'équipe peuvent traiter une demande");
        }

        AffiliationEnseignant affiliation = affiliationRepository.findByIdAndEquipeId(affiliationId, equipeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande d'affiliation introuvable (id=" + affiliationId + ")"));

        EquipeRecherche equipe = affiliation.getEquipe();

        if (decider.getRole() != Role.ROLE_ADMIN
                && (equipe.getChef() == null || !equipe.getChef().getId().equals(decider.getId()))) {
            throw new BusinessException("Vous n'êtes pas le chef de cette équipe");
        }

        if (affiliation.getStatut() != StatutAffiliation.EN_ATTENTE) {
            throw new BusinessException("Cette demande a déjà été traitée");
        }

        affiliation.setStatut(statut);
        affiliation.setDateDecision(LocalDateTime.now());
        affiliation.setMotifDecision(motifDecision);

        if (statut == StatutAffiliation.ACCEPTEE) {
            User enseignant = affiliation.getEnseignant();
            if (enseignant.getEquipeRecherche() != null
                    && !enseignant.getEquipeRecherche().getId().equals(equipeId)) {
                throw new BusinessException("L'enseignant appartient déjà à une autre équipe");
            }
            equipe.addMember(enseignant);
            equipeRepository.save(equipe);
            userRepository.save(enseignant);

            List<AffiliationEnseignant> autresDemandes = affiliationRepository
                    .findByEnseignantIdAndStatut(enseignant.getId(), StatutAffiliation.EN_ATTENTE);
            for (AffiliationEnseignant autre : autresDemandes) {
                autre.setStatut(StatutAffiliation.REFUSEE);
                autre.setDateDecision(LocalDateTime.now());
                autre.setMotifDecision("Déjà accepté dans une autre équipe");
            }
            affiliationRepository.saveAll(autresDemandes);
        }

        return toResponse(affiliationRepository.save(affiliation));
    }

    @Transactional(readOnly = true)
    public List<AffiliationEnseignantResponse> getDemandesByEquipe(Long equipeId) {
        return affiliationRepository.findByEquipeIdOrderByDateDemandeDesc(equipeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AffiliationEnseignantResponse> getAllDemandes() {
        return affiliationRepository.findAllByOrderByDateDemandeDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AffiliationEnseignantResponse> getMesDemandes(User enseignant) {
        return affiliationRepository.findByEnseignantIdOrderByDateDemandeDesc(enseignant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AffiliationEnseignantResponse toResponse(AffiliationEnseignant affiliation) {
        UserResponse enseignantResponse = userMapper.toResponse(affiliation.getEnseignant());
        return new AffiliationEnseignantResponse(
                affiliation.getId(),
                enseignantResponse,
                affiliation.getEquipe().getId(),
                affiliation.getEquipe().getNom(),
                affiliation.getStatut(),
                affiliation.getDateDemande(),
                affiliation.getDateDecision(),
                affiliation.getMotifDecision()
        );
    }
}

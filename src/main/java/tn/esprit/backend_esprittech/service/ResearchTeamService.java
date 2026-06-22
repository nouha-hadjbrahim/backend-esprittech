package tn.esprit.backend_esprittech.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.backend_esprittech.dto.request.CreateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateResearchTeamRequest;
import tn.esprit.backend_esprittech.dto.response.ResearchTeamResponse;
import tn.esprit.backend_esprittech.entity.ResearchTeam;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.ResearchTeamMapper;
import tn.esprit.backend_esprittech.repository.ResearchTeamRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Service
public class ResearchTeamService {

    private final ResearchTeamRepository researchTeamRepository;
    private final UserRepository userRepository;
    private final ResearchTeamMapper researchTeamMapper;

    public ResearchTeamService(ResearchTeamRepository researchTeamRepository,
                               UserRepository userRepository,
                               ResearchTeamMapper researchTeamMapper) {
        this.researchTeamRepository = researchTeamRepository;
        this.userRepository = userRepository;
        this.researchTeamMapper = researchTeamMapper;
    }

    @Transactional
    public ResearchTeamResponse create(CreateResearchTeamRequest request) {
        if (researchTeamRepository.existsByNom(request.nom())) {
            throw new BusinessException("Le nom de l'équipe existe déjà");
        }

        User leader = null;
        if (request.chefId() != null) {
            leader = findEnseignant(request.chefId(), "Chef introuvable");
            ensureUserIsAvailableAsLeader(leader, null);
        }

        ResearchTeam researchTeam = researchTeamMapper.toEntity(request);
        researchTeam.setLeader(leader);

        assignMembers(researchTeam, request.memberIds(), leader);

        return researchTeamMapper.toResponse(researchTeamRepository.save(researchTeam));
    }

    @Transactional
    public ResearchTeamResponse update(Long id, UpdateResearchTeamRequest request) {
        ResearchTeam researchTeam = findTeam(id);

        if (request.nom() != null && researchTeamRepository.existsByNomAndIdNot(request.nom(), id)) {
            throw new BusinessException("Le nom de l'équipe existe déjà");
        }

        User currentLeader = researchTeam.getLeader();
        User leader = currentLeader;
        if (request.chefId() != null) {
            leader = findEnseignant(request.chefId(), "Chef introuvable");
            ensureUserIsAvailableAsLeader(leader, id);
        }

        researchTeamMapper.updateEntity(request, researchTeam);
        researchTeam.setLeader(leader);

        if (request.chefId() != null && researchTeam.getMembers().contains(leader)) {
            researchTeam.removeMember(leader);
        }

        if (request.memberIds() != null) {
            clearMembers(researchTeam);
            assignMembers(researchTeam, request.memberIds(), leader);
        }

        return researchTeamMapper.toResponse(researchTeamRepository.save(researchTeam));
    }

    @Transactional(readOnly = true)
    public List<ResearchTeamResponse> findAll() {
        return researchTeamRepository.findAll().stream()
                .map(researchTeamMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResearchTeamResponse findById(Long id) {
        return researchTeamMapper.toResponse(findTeam(id));
    }

    @Transactional
    public void delete(Long id) {
        ResearchTeam researchTeam = findTeam(id);
        clearMembers(researchTeam);
        researchTeamRepository.delete(Objects.requireNonNull(researchTeam, "researchTeam"));
    }

    private ResearchTeam findTeam(Long id) {
        Long teamId = Objects.requireNonNull(id, "id");
        return researchTeamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipe introuvable (id=" + id + ")"));
    }

    private User findEnseignant(Long id, String notFoundMessage) {
        Long userId = Objects.requireNonNull(id, "id");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage + " (id=" + id + ")"));
        if (user.getRole() != Role.ROLE_ENSEIGNANT) {
            throw new BusinessException("Le chef et les membres doivent avoir le rôle ENSEIGNANT");
        }
        return user;
    }

    private void ensureUserIsAvailableAsLeader(User user, Long currentTeamId) {
        if (researchTeamRepository.existsByLeaderIdAndIdNot(user.getId(), currentTeamId == null ? -1L : currentTeamId)) {
            throw new BusinessException("Cet enseignant est déjà chef d'une autre équipe");
        }
        if (user.getResearchTeam() != null && (currentTeamId == null || !currentTeamId.equals(user.getResearchTeam().getId()))) {
            throw new BusinessException("Cet enseignant appartient déjà à une équipe");
        }
    }

    private void assignMembers(ResearchTeam researchTeam, List<Long> memberIds, User leader) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new LinkedHashSet<>(memberIds);
        if (leader != null && uniqueIds.contains(leader.getId())) {
            throw new BusinessException("Le chef ne peut pas figurer dans la liste des membres");
        }

        List<User> members = new ArrayList<>(userRepository.findAllByIdIn(new ArrayList<>(uniqueIds)));
        if (members.size() != uniqueIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs membres sont introuvables");
        }

        for (User user : members) {
            if (user.getRole() != Role.ROLE_ENSEIGNANT) {
                throw new BusinessException("Les membres doivent avoir le rôle ENSEIGNANT");
            }
            if (user.getResearchTeam() != null && !user.getResearchTeam().equals(researchTeam)) {
                throw new BusinessException("Un ou plusieurs membres appartiennent déjà à une autre équipe");
            }
        }

        for (User user : members) {
            researchTeam.addMember(user);
        }
    }

    private void clearMembers(ResearchTeam researchTeam) {
        List<User> existingMembers = new ArrayList<>(Objects.requireNonNull(researchTeam, "researchTeam").getMembers());
        for (User user : existingMembers) {
            researchTeam.removeMember(user);
        }
    }
}
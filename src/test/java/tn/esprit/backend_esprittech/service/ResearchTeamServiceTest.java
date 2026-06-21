package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchTeamServiceTest {

    @Mock private ResearchTeamRepository researchTeamRepository;
    @Mock private UserRepository userRepository;
    @Mock private ResearchTeamMapper researchTeamMapper;

    @InjectMocks
    private ResearchTeamService service;

    // ── shared fixtures ────────────────────────────────────────────────────────

    private User enseignant(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ROLE_ENSEIGNANT);
        return u;
    }

    private ResearchTeam team(long id) {
        ResearchTeam t = new ResearchTeam();
        t.setId(id);
        return t;
    }

    private ResearchTeamResponse response() {
        return mock(ResearchTeamResponse.class);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  create()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void create_success_noMembers() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, null);
        User leader = enseignant(1L);
        ResearchTeam entity = team(10L);
        ResearchTeamResponse resp = response();

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(researchTeamRepository.save(entity)).thenReturn(entity);
        when(researchTeamMapper.toResponse(entity)).thenReturn(resp);

        assertThat(service.create(req)).isSameAs(resp);
        verify(researchTeamRepository).save(entity);
    }

    @Test
    void create_success_withMembers() {
        User leader = enseignant(1L);
        User member = enseignant(2L);
        ResearchTeam entity = team(10L);
        ResearchTeamResponse resp = response();

        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(2L));

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(userRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(member));
        when(researchTeamRepository.save(entity)).thenReturn(entity);
        when(researchTeamMapper.toResponse(entity)).thenReturn(resp);

        assertThat(service.create(req)).isSameAs(resp);
    }

    @Test
    void create_throws_whenDomaineAlreadyExists() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, null);
        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("domaine de recherche");
    }

    @Test
    void create_throws_whenLeaderNotFound() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 99L, null);
        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_throws_whenLeaderNotEnseignant() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, null);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ROLE_ADMIN);

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ENSEIGNANT");
    }

    @Test
    void create_throws_whenLeaderAlreadyLeadsAnotherTeam() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, null);
        User leader = enseignant(1L);

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà chef");
    }

    @Test
    void create_throws_whenLeaderAlreadyBelongsToATeam() {
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, null);
        User leader = enseignant(1L);
        leader.setResearchTeam(team(99L));   // belongs to a different team

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("appartient déjà");
    }

    @Test
    void create_throws_whenLeaderInMemberList() {
        User leader = enseignant(1L);
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(1L));
        ResearchTeam entity = team(10L);

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chef ne peut pas figurer");
    }

    @Test
    void create_throws_whenMemberNotFound() {
        User leader = enseignant(1L);
        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(2L));
        ResearchTeam entity = team(10L);

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(userRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of()); // none returned

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("introuvables");
    }

    @Test
    void create_throws_whenMemberNotEnseignant() {
        User leader = enseignant(1L);
        User nonEnseignant = new User();
        nonEnseignant.setId(2L);
        nonEnseignant.setRole(Role.ROLE_ADMIN);

        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(2L));
        ResearchTeam entity = team(10L);

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(userRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(nonEnseignant));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rôle ENSEIGNANT");
    }

    @Test
    void create_throws_whenMemberAlreadyInAnotherTeam() {
        User leader = enseignant(1L);
        User member = enseignant(2L);
        member.setResearchTeam(team(99L)); // another team

        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(2L));
        ResearchTeam entity = team(10L); // different team

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(userRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(member));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("appartiennent déjà");
    }

    // Member whose researchTeam IS the current team — no exception
    @Test
    void create_memberAlreadyInSameTeam_isAllowed() {
        User leader = enseignant(1L);
        ResearchTeam entity = team(10L);
        User member = enseignant(2L);
        member.setResearchTeam(entity); // already in THIS team → ok

        CreateResearchTeamRequest req = new CreateResearchTeamRequest("Team IA", "IA", "desc", 1L, List.of(2L));
        ResearchTeamResponse resp = response();

        when(researchTeamRepository.existsByDomaineRecherche("IA")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(1L, -1L)).thenReturn(false);
        when(researchTeamMapper.toEntity(req)).thenReturn(entity);
        when(userRepository.findAllByIdIn(List.of(2L))).thenReturn(List.of(member));
        when(researchTeamRepository.save(entity)).thenReturn(entity);
        when(researchTeamMapper.toResponse(entity)).thenReturn(resp);

        assertThat(service.create(req)).isSameAs(resp);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  update()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void update_success_noChanges() {
        ResearchTeam existing = team(1L);
        User currentLeader = enseignant(5L);
        existing.setLeader(currentLeader);

        UpdateResearchTeamRequest req = new UpdateResearchTeamRequest(null, null, null, null, null);
        ResearchTeamResponse resp = response();

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(researchTeamRepository.save(existing)).thenReturn(existing);
        when(researchTeamMapper.toResponse(existing)).thenReturn(resp);

        assertThat(service.update(1L, req)).isSameAs(resp);
    }

    @Test
    void update_success_newLeaderAndMembers() {
        ResearchTeam existing = team(1L);
        User oldLeader = enseignant(5L);
        existing.setLeader(oldLeader);

        User newLeader = enseignant(6L);
        User member = enseignant(7L);
        ResearchTeamResponse resp = response();

        UpdateResearchTeamRequest req = new UpdateResearchTeamRequest(null, "Bio", null, 6L, List.of(7L));

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(researchTeamRepository.existsByDomaineRechercheAndIdNot("Bio", 1L)).thenReturn(false);
        when(userRepository.findById(6L)).thenReturn(Optional.of(newLeader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(6L, 1L)).thenReturn(false);
        when(userRepository.findAllByIdIn(List.of(7L))).thenReturn(List.of(member));
        when(researchTeamRepository.save(existing)).thenReturn(existing);
        when(researchTeamMapper.toResponse(existing)).thenReturn(resp);

        assertThat(service.update(1L, req)).isSameAs(resp);
    }

    @Test
    void update_throws_whenTeamNotFound() {
        when(researchTeamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new UpdateResearchTeamRequest(null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_throws_whenDomaineAlreadyUsed() {
        ResearchTeam existing = team(1L);
        existing.setLeader(enseignant(5L));

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(researchTeamRepository.existsByDomaineRechercheAndIdNot("Bio", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UpdateResearchTeamRequest(null, "Bio", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("domaine de recherche");
    }

    @Test
    void update_throws_whenNewLeaderNotFound() {
        ResearchTeam existing = team(1L);
        existing.setLeader(enseignant(5L));

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, new UpdateResearchTeamRequest(null, null, null, 99L, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_throws_whenNewLeaderAlreadyLeadsAnotherTeam() {
        ResearchTeam existing = team(1L);
        existing.setLeader(enseignant(5L));
        User newLeader = enseignant(6L);

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newLeader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(6L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UpdateResearchTeamRequest(null, null, null, 6L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà chef");
    }

    // New leader is already a member of THIS team → should be removed from members
    @Test
    void update_newLeaderRemovedFromMembers() {
        ResearchTeam existing = team(1L);
        User oldLeader = enseignant(5L);
        User newLeader = enseignant(6L);
        existing.setLeader(oldLeader);
        existing.addMember(newLeader); // newLeader is currently a member

        ResearchTeamResponse resp = response();
        UpdateResearchTeamRequest req = new UpdateResearchTeamRequest(null, null, null, 6L, null);

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newLeader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(6L, 1L)).thenReturn(false);
        when(researchTeamRepository.save(existing)).thenReturn(existing);
        when(researchTeamMapper.toResponse(existing)).thenReturn(resp);

        assertThat(service.update(1L, req)).isSameAs(resp);
        assertThat(existing.getMembers()).doesNotContain(newLeader);
    }

    // New leader belongs to a different team → exception
    @Test
    void update_throws_whenNewLeaderBelongsToAnotherTeam() {
        ResearchTeam existing = team(1L);
        existing.setLeader(enseignant(5L));

        User newLeader = enseignant(6L);
        newLeader.setResearchTeam(team(99L)); // different team

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newLeader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(6L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.update(1L, new UpdateResearchTeamRequest(null, null, null, 6L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("appartient déjà");
    }

    // Leader belongs to current team → not an error
    @Test
    void update_newLeaderBelongsToCurrentTeam_isAllowed() {
        ResearchTeam existing = team(1L);
        User oldLeader = enseignant(5L);
        existing.setLeader(oldLeader);

        User newLeader = enseignant(6L);
        newLeader.setResearchTeam(existing); // same team

        ResearchTeamResponse resp = response();
        UpdateResearchTeamRequest req = new UpdateResearchTeamRequest(null, null, null, 6L, null);

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(6L)).thenReturn(Optional.of(newLeader));
        when(researchTeamRepository.existsByLeaderIdAndIdNot(6L, 1L)).thenReturn(false);
        when(researchTeamRepository.save(existing)).thenReturn(existing);
        when(researchTeamMapper.toResponse(existing)).thenReturn(resp);

        assertThat(service.update(1L, req)).isSameAs(resp);
    }

    @Test
    void update_clearsAndReassignsMembers_emptyList() {
        ResearchTeam existing = team(1L);
        User leader = enseignant(5L);
        User existingMember = enseignant(8L);
        existing.setLeader(leader);
        existing.addMember(existingMember);

        UpdateResearchTeamRequest req = new UpdateResearchTeamRequest(null, null, null, null, new ArrayList<>());
        ResearchTeamResponse resp = response();

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(researchTeamRepository.save(existing)).thenReturn(existing);
        when(researchTeamMapper.toResponse(existing)).thenReturn(resp);

        assertThat(service.update(1L, req)).isSameAs(resp);
        assertThat(existing.getMembers()).isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  findAll()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void findAll_returnsAll() {
        ResearchTeam t1 = team(1L);
        ResearchTeam t2 = team(2L);
        ResearchTeamResponse r1 = response();
        ResearchTeamResponse r2 = response();

        when(researchTeamRepository.findAll()).thenReturn(List.of(t1, t2));
        when(researchTeamMapper.toResponse(t1)).thenReturn(r1);
        when(researchTeamMapper.toResponse(t2)).thenReturn(r2);

        assertThat(service.findAll()).containsExactly(r1, r2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  findById()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void findById_success() {
        ResearchTeam t = team(1L);
        ResearchTeamResponse resp = response();

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(t));
        when(researchTeamMapper.toResponse(t)).thenReturn(resp);

        assertThat(service.findById(1L)).isSameAs(resp);
    }

    @Test
    void findById_throws_whenNotFound() {
        when(researchTeamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  delete()
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void delete_success_noMembers() {
        ResearchTeam t = team(1L);
        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(t));

        service.delete(1L);

        verify(researchTeamRepository).delete(t);
    }

    @Test
    void delete_success_withMembers() {
        ResearchTeam t = team(1L);
        User m1 = enseignant(2L);
        User m2 = enseignant(3L);
        t.addMember(m1);
        t.addMember(m2);

        when(researchTeamRepository.findById(1L)).thenReturn(Optional.of(t));

        service.delete(1L);

        verify(researchTeamRepository).delete(t);
        assertThat(t.getMembers()).isEmpty();
    }

    @Test
    void delete_throws_whenNotFound() {
        when(researchTeamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
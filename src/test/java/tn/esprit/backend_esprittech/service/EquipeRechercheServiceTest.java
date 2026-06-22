package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import tn.esprit.backend_esprittech.dto.request.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.response.EquipeResponse;
import tn.esprit.backend_esprittech.entity.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.EquipeMapper;
import tn.esprit.backend_esprittech.repository.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipeRechercheServiceTest {

    @Mock private EquipeRechercheRepository equipeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EquipeMapper equipeMapper;

    @InjectMocks private EquipeRechercheService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User enseignant(Long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ROLE_ENSEIGNANT);
        return u;
    }

    private User chefEquipe(Long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ROLE_CHEF_EQUIPE);
        return u;
    }

    private EquipeRecherche equipeVide() {
        EquipeRecherche e = new EquipeRecherche();
        e.setId(1L);
        e.setMembers(new ArrayList<>());
        return e;
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Test
    void create_duplicateName_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(equipeRepository.existsByNom("AI")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_noChef_noMembers_success() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(null);

        EquipeRecherche saved = equipeVide();
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.create(req));
        verify(equipeRepository).save(any());
    }

    @Test
    void create_withChef_promotesToChefEquipe() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        User chef = enseignant(10L);
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(false);
        EquipeRecherche saved = equipeVide();
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        service.create(req);

        assertEquals(Role.ROLE_CHEF_EQUIPE, chef.getRole());
    }

    @Test
    void create_chefAlreadyLeadsAnotherTeam_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(10L);

        User chef = enseignant(10L);
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_chefAlreadyMemberOfAnotherTeam_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(10L);

        User chef = enseignant(10L);
        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);
        chef.setEquipeRecherche(autreEquipe);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_chefNotFound_throwsResourceNotFoundException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(10L);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(req));
    }

    @Test
    void create_chefInvalidRole_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(10L);

        User chef = new User();
        chef.setId(10L);
        chef.setRole(Role.ROLE_ADMIN);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_withChefEquipeRole_allowed() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        User chef = chefEquipe(10L);
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(false);
        EquipeRecherche saved = equipeVide();
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.create(req));
    }

    @Test
    void create_withMembers_success() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L, 21L));

        User m1 = enseignant(20L);
        User m2 = enseignant(21L);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(m1, m2));
        EquipeRecherche saved = equipeVide();
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.create(req));
    }

    @Test
    void create_memberIsChef_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(List.of(10L));

        User chef = enseignant(10L);
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_memberNotFound_throwsResourceNotFoundException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L));

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.create(req));
    }

    @Test
    void create_memberInvalidRole_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L));

        User m = new User();
        m.setId(20L);
        m.setRole(Role.ROLE_ADMIN);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(m));

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void create_memberAlreadyInAnotherTeam_throwsBusinessException() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L));

        User m = enseignant(20L);
        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);
        m.setEquipeRecherche(autreEquipe);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(m));

        assertThrows(BusinessException.class, () -> service.create(req));
    }

    // =========================================================================
    // update()
    // =========================================================================

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, mock(UpdateEquipeRechercheRequest.class)));
    }

    @Test
    void update_duplicateName_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.existsByNomAndIdNot("AI", 1L)).thenReturn(true);

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");

        assertThrows(BusinessException.class, () -> service.update(1L, req));
    }

    @Test
    void update_noChanges_success() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(null);

        assertDoesNotThrow(() -> service.update(1L, req));
    }

    @Test
    void update_newChef_removedFromMembersIfPresent() {
        User chef = enseignant(10L);
        EquipeRecherche e = equipeVide();
        e.getMembers().add(chef);
        chef.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, 1L)).thenReturn(false);
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        service.update(1L, req);

        assertFalse(e.getMembers().contains(chef));
    }

    @Test
    void update_withNewMembers_clearsAndAssigns() {
        EquipeRecherche e = equipeVide();
        User existing = enseignant(30L);
        e.getMembers().add(existing);
        existing.setEquipeRecherche(e);

        User newMember = enseignant(31L);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(newMember));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(31L));

        service.update(1L, req);

        verify(equipeRepository).save(e);
    }

    // =========================================================================
    // assignerChef()
    // =========================================================================

    @Test
    void assignerChef_notFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.assignerChef(1L, 2L));
    }

    @Test
    void assignerChef_invalidRole_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User u = new User();
        u.setId(2L);
        u.setRole(Role.ROLE_ADMIN);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u));

        assertThrows(BusinessException.class, () -> service.assignerChef(1L, 2L));
    }

    @Test
    void assignerChef_valid_savesAndReturns() {
        EquipeRecherche e = equipeVide();
        User chef = enseignant(2L);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(2L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(2L, 1L)).thenReturn(false);
        when(equipeRepository.save(e)).thenReturn(e);

        EquipeRecherche result = service.assignerChef(1L, 2L);

        assertEquals(chef, result.getChef());
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Test
    void delete_notFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(1L));
    }

    @Test
    void delete_withMembers_dissociatesAndDeletes() {
        EquipeRecherche e = equipeVide();
        User m = enseignant(20L);
        e.getMembers().add(m);
        m.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));

        service.delete(1L);

        assertNull(m.getEquipeRecherche());
        verify(equipeRepository).delete(e);
    }

    @Test
    void delete_noMembers_deletesDirectly() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));

        service.delete(1L);

        verify(equipeRepository).delete(e);
    }

    // =========================================================================
    // listerToutes()
    // =========================================================================

    @Test
    void listerToutes_returnsAll() {
        List<EquipeRecherche> list = List.of(equipeVide(), equipeVide());
        when(equipeRepository.findAll()).thenReturn(list);

        assertEquals(2, service.listerToutes().size());
    }

    // =========================================================================
    // findById()
    // =========================================================================

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(1L));
    }

    @Test
    void findById_found_returnsResponse() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.findById(1L));
    }

    // =========================================================================
    // getEntite()
    // =========================================================================

    @Test
    void getEntite_notFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getEntite(1L));
    }

    @Test
    void getEntite_found_returnsEntity() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        assertEquals(e, service.getEntite(1L));
    }

    // =========================================================================
    // getMembres()
    // =========================================================================

    @Test
    void getMembres_withChefAndMembers_returnsAll() {
        EquipeRecherche e = equipeVide();
        User chef = enseignant(1L);
        User member = enseignant(2L);
        e.setChef(chef);
        e.getMembers().add(member);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));

        List<User> result = service.getMembres(1L);

        assertEquals(2, result.size());
        assertEquals(chef, result.get(0));
    }

    @Test
    void getMembres_noChef_returnsMembersOnly() {
        EquipeRecherche e = equipeVide();
        User member = enseignant(2L);
        e.getMembers().add(member);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));

        List<User> result = service.getMembres(1L);

        assertEquals(1, result.size());
    }

    // =========================================================================
    // compterMembres()
    // =========================================================================

    @Test
    void compterMembres_noChefNoMembers_returnsZero() {
        EquipeRecherche e = equipeVide();
        assertEquals(0, service.compterMembres(e));
    }

    @Test
    void compterMembres_chefOnly_returnsOne() {
        EquipeRecherche e = equipeVide();
        e.setChef(new User());
        assertEquals(1, service.compterMembres(e));
    }

    @Test
    void compterMembres_chefAndTwoMembers_returnsThree() {
        EquipeRecherche e = equipeVide();
        e.setChef(new User());
        e.setMembers(List.of(new User(), new User()));
        assertEquals(3, service.compterMembres(e));
    }

    @Test
    void compterMembres_nullMembers_countsOnlyChef() {
        EquipeRecherche e = new EquipeRecherche();
        e.setId(1L);
        e.setChef(new User());
        e.setMembers(null);
        assertEquals(1, service.compterMembres(e));
    }


    // Branch: update() → request.nom() != null but name is NOT duplicate (existsByNomAndIdNot = false)
    @Test
    void update_nomNotDuplicate_proceedsNormally() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.existsByNomAndIdNot("NewName", 1L)).thenReturn(false);
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("NewName");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(null);

        assertDoesNotThrow(() -> service.update(1L, req));
    }

    // Branch: update() → chefId != null but chef is NOT in members (contains = false)
    @Test
    void update_newChef_notInMembers_doesNotRemove() {
        User chef = enseignant(10L);
        EquipeRecherche e = equipeVide();
        // chef is NOT in members list

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, 1L)).thenReturn(false);
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        service.update(1L, req);

        // members should still be empty — nothing removed
        assertTrue(e.getMembers().isEmpty());
    }

    // Branch: ensureChefDisponible() → chef.getEquipeRecherche() != null
    //         but currentEquipeId.equals(chef.getEquipeRecherche().getId()) → same team → no exception
    @Test
    void update_chefAlreadyInSameTeam_noException() {
        EquipeRecherche e = equipeVide(); // id = 1L

        User chef = enseignant(10L);
        chef.setEquipeRecherche(e); // chef belongs to THIS team → ok

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, 1L)).thenReturn(false);
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        assertDoesNotThrow(() -> service.update(1L, req));
    }

    // Branch: assignerMembres() → memberIds is empty list → early return
    @Test
    void create_emptyMemberIds_skipsAssignment() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(new ArrayList<>()); // empty, not null

        EquipeRecherche saved = equipeVide();
        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.create(req));
        verifyNoInteractions(userRepository); // no member lookup
    }

    // Branch: assignerMembres() → chef != null but chef.getId() NOT in uniqueIds → no exception
    @Test
    void create_membersDoNotIncludeChef_success() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(List.of(20L)); // 20L != 10L

        User chef = enseignant(10L);
        User member = enseignant(20L);

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, -1L)).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));
        EquipeRecherche saved = equipeVide();
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.create(req));
    }

    // Branch: assignerMembres() → member.getEquipeRecherche() == null → no conflict
    @Test
    void create_memberWithNoExistingTeam_success() {
        CreateEquipeRechercheRequest req = mock(CreateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn("AI");
        when(req.description()).thenReturn("Desc");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L));

        User member = enseignant(20L);
        member.setEquipeRecherche(null); // explicitly no team

        when(equipeRepository.existsByNom("AI")).thenReturn(false);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));
        EquipeRecherche saved = equipeVide();
        when(equipeRepository.save(any())).thenReturn(saved);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.create(req));
    }

    // Branch: assignerMembres() → member.getEquipeRecherche() == same equipe → no conflict
    @Test
    void update_memberAlreadyInSameTeam_noException() {
        EquipeRecherche e = equipeVide();
        User member = enseignant(20L);
        member.setEquipeRecherche(e); // same team → allowed

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(List.of(20L));

        assertDoesNotThrow(() -> service.update(1L, req));
    }
}
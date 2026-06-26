package tn.esprit.backend_esprittech.service.equipe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import tn.esprit.backend_esprittech.dto.equipe.CreateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.UpdateEquipeRechercheRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeResponse;
import tn.esprit.backend_esprittech.entity.equipe.EquipeRecherche;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.mapper.equipe.EquipeMapper;
import tn.esprit.backend_esprittech.repository.equipe.EquipeRechercheRepository;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipeRechercheServiceTest {

    @Mock
    private EquipeRechercheRepository equipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EquipeMapper equipeMapper;

    @InjectMocks
    private EquipeRechercheService service;

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

    // Branch: ensureChefDisponible() → chef.getEquipeRecherche() != null
    // AND currentEquipeId != null
    // AND currentEquipeId != chef.getEquipeRecherche().getId() → exception
    @Test
    void update_chefBelongsToAnotherTeam_throwsBusinessException() {
        EquipeRecherche currentEquipe = equipeVide(); // id = 1L

        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L); // different team

        User chef = enseignant(10L);
        chef.setEquipeRecherche(autreEquipe); // chef belongs to team 99L, not 1L

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(currentEquipe));
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(10L, 1L)).thenReturn(false);
        // existsByChefIdAndIdNot = false so we pass that guard,
        // then hit the getEquipeRecherche() != null AND 1L != 99L → exception

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(10L);
        when(req.memberIds()).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.update(1L, req));
    }

    // Branch: assignerChef() → ensureChefDisponible() → chef.getEquipeRecherche()
    // != null
    // AND equipeId (2L) != chef.getEquipeRecherche().getId() (99L) → exception
    @Test
    void assignerChef_chefBelongsToAnotherTeam_throwsBusinessException() {
        EquipeRecherche equipe = equipeVide(); // id = 1L

        EquipeRecherche autreEquipe = new EquipeRecherche();
        autreEquipe.setId(99L);

        User chef = enseignant(2L);
        chef.setEquipeRecherche(autreEquipe); // belongs to team 99L, not 1L

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(equipe));
        when(userRepository.findById(2L)).thenReturn(Optional.of(chef));
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeRepository.existsByChefIdAndIdNot(2L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.assignerChef(1L, 2L));
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
        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        assertThrows(ResourceNotFoundException.class,
                () -> service.update(1L, req));
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
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.assignerChef(1L, 2L));
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
    void delete_withMembers_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User m = enseignant(20L);
        e.getMembers().add(m);
        m.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(equipeRepository, never()).delete(any());
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
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

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

    @Test
    void findById_withNullMembers_handlesGracefully() {
        EquipeRecherche e = new EquipeRecherche();
        e.setId(1L);
        e.setMembers(null);
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.findById(1L));
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

    // Branch: update() → request.nom() != null but name is NOT duplicate
    // (existsByNomAndIdNot = false)
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

    @Test
    void update_domaineProvided_updatesEntity() {
        EquipeRecherche e = equipeVide();
        e.setDomaine("OldDomaine");
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.description()).thenReturn(null);
        when(req.domaine()).thenReturn("NewDomaine");
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(null);

        service.update(1L, req);

        verify(equipeMapper).updateEntity(req, e);
    }

    // Branch: update() → chefId != null but chef is NOT in members (contains =
    // false)
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
    // but currentEquipeId.equals(chef.getEquipeRecherche().getId()) → same team →
    // no exception
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

    // Branch: update() → chefId == null but equipe has a chef → chef is demoted and removed
    @Test
    void update_removeChef_demotesToEnseignant() {
        EquipeRecherche e = equipeVide();
        User oldChef = chefEquipe(10L);
        e.setChef(oldChef);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(userRepository.save(oldChef)).thenReturn(oldChef);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(null);
        when(req.memberIds()).thenReturn(null);

        service.update(1L, req);

        assertNull(e.getChef());
        assertEquals(Role.ROLE_ENSEIGNANT, oldChef.getRole());
        verify(userRepository).save(oldChef);
    }

    // Branch: update() → chefId == null, equipe has chef AND members,
    // memberIds keeps current members (ex-chef becomes regular member after demotion)
    @Test
    void update_removeChef_keepsMembersIntact() {
        EquipeRecherche e = equipeVide();
        User oldChef = chefEquipe(10L);
        User member1 = enseignant(20L);
        User member2 = enseignant(21L);
        e.setChef(oldChef);
        e.getMembers().add(oldChef);
        oldChef.setEquipeRecherche(e);
        e.getMembers().add(member1);
        member1.setEquipeRecherche(e);
        e.getMembers().add(member2);
        member2.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(userRepository.save(oldChef)).thenReturn(oldChef);
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(oldChef, member1, member2));
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        UpdateEquipeRechercheRequest req = mock(UpdateEquipeRechercheRequest.class);
        when(req.nom()).thenReturn(null);
        when(req.chefId()).thenReturn(null);                       // chef removed
        when(req.memberIds()).thenReturn(List.of(10L, 20L, 21L));   // members kept (including ex-chef)

        service.update(1L, req);

        assertNull(e.getChef());                                          // no chef
        assertEquals(Role.ROLE_ENSEIGNANT, oldChef.getRole());             // demoted
        assertEquals(3, e.getMembers().size());                            // all 3 still in members
        assertTrue(e.getMembers().contains(oldChef));                       // ex-chef now a regular member
        verify(userRepository).save(oldChef);
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

    // Branch: assignerMembres() → chef != null but chef.getId() NOT in uniqueIds →
    // no exception
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

    // Branch: assignerMembres() → member.getEquipeRecherche() == same equipe → no
    // conflict
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

    // =========================================================================
    // ajouterMembres()
    // =========================================================================

    @Test
    void ajouterMembres_teamNotFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(99L)).thenReturn(Optional.empty());
        List<Long> memberIds = List.of(1L);
        assertThrows(ResourceNotFoundException.class,
                () -> service.ajouterMembres(99L, memberIds));
    }

    @Test
    void ajouterMembres_emptyMemberIds_noOp() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertDoesNotThrow(() -> service.ajouterMembres(1L, List.of()));
        verifyNoInteractions(userRepository);
    }

    @Test
    void ajouterMembres_valid_savesAndReturns() {
        EquipeRecherche e = equipeVide();
        User member = enseignant(20L);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.ajouterMembres(1L, List.of(20L)));
        assertTrue(e.getMembers().contains(member));
    }

    @Test
    void ajouterMembres_memberAlreadyInAnotherTeam_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User member = enseignant(20L);
        EquipeRecherche autre = new EquipeRecherche();
        autre.setId(99L);
        member.setEquipeRecherche(autre);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));
        List<Long> memberIds = List.of(20L);

        assertThrows(BusinessException.class,
                () -> service.ajouterMembres(1L, memberIds));
    }

    @Test
    void ajouterMembres_memberNotFound_throwsResourceNotFoundException() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of());
        List<Long> memberIds = List.of(20L);

        assertThrows(ResourceNotFoundException.class,
                () -> service.ajouterMembres(1L, memberIds));
    }

    @Test
    void ajouterMembres_memberInvalidRole_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User member = new User();
        member.setId(20L);
        member.setRole(Role.ROLE_ADMIN);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findAllByIdIn(any())).thenReturn(List.of(member));
        List<Long> memberIds = List.of(20L);

        assertThrows(BusinessException.class,
                () -> service.ajouterMembres(1L, memberIds));
    }

    @Test
    void ajouterMembres_chefInMemberIds_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User chef = enseignant(10L);
        e.setChef(chef);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        List<Long> memberIds = List.of(10L);

        assertThrows(BusinessException.class,
                () -> service.ajouterMembres(1L, memberIds));
    }

    // =========================================================================
    // retirerMembre()
    // =========================================================================

    @Test
    void retirerMembre_teamNotFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.retirerMembre(99L, 1L));
    }

    @Test
    void retirerMembre_userNotFound_throwsResourceNotFoundException() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.retirerMembre(1L, 99L));
    }

    @Test
    void retirerMembre_userNotAMember_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User user = enseignant(20L);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class,
                () -> service.retirerMembre(1L, 20L));
    }

    @Test
    void retirerMembre_removeChef_throwsBusinessException() {
        EquipeRecherche e = equipeVide();
        User chef = enseignant(10L);
        e.setChef(chef);
        e.getMembers().add(chef);
        chef.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(10L)).thenReturn(Optional.of(chef));

        assertThrows(BusinessException.class,
                () -> service.retirerMembre(1L, 10L));
    }

    @Test
    void retirerMembre_withChef_removeNonChefMember_removesAndSaves() {
        EquipeRecherche e = equipeVide();
        User chef = enseignant(10L);
        User member = enseignant(20L);
        e.setChef(chef);
        e.getMembers().add(chef);
        chef.setEquipeRecherche(e);
        e.getMembers().add(member);
        member.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(20L)).thenReturn(Optional.of(member));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.retirerMembre(1L, 20L));
        assertFalse(e.getMembers().contains(member));
        assertNull(member.getEquipeRecherche());
        assertTrue(e.getMembers().contains(chef));
    }

    @Test
    void retirerMembre_valid_removesAndSaves() {
        EquipeRecherche e = equipeVide();
        User member = enseignant(20L);
        e.getMembers().add(member);
        member.setEquipeRecherche(e);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(20L)).thenReturn(Optional.of(member));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.retirerMembre(1L, 20L));
        assertFalse(e.getMembers().contains(member));
        assertNull(member.getEquipeRecherche());
    }

    // =========================================================================
    // assignerChef() — missing branch
    // =========================================================================

    @Test
    void assignerChef_userNotFound_throwsResourceNotFoundException() {
        EquipeRecherche e = equipeVide();
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.assignerChef(1L, 99L));
    }

    // =========================================================================
    // retirerChef()
    // =========================================================================

    @Test
    void retirerChef_existingTeam_setsChefNullAndSaves() {
        EquipeRecherche e = equipeVide();
        User chef = chefEquipe(1L);
        e.setChef(chef);

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(userRepository.save(chef)).thenReturn(chef);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.retirerChef(1L));
        assertNull(e.getChef());
        assertEquals(Role.ROLE_ENSEIGNANT, chef.getRole());
        verify(equipeRepository).save(e);
        verify(userRepository).save(chef);
    }

    @Test
    void retirerChef_teamNotFound_throwsResourceNotFoundException() {
        when(equipeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.retirerChef(99L));
    }

    @Test
    void retirerChef_noChef_skipsUserSave() {
        EquipeRecherche e = equipeVide();
        e.setChef(null); // no chef

        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(equipeRepository.save(e)).thenReturn(e);
        when(equipeMapper.toResponse(any(), anyInt())).thenReturn(mock(EquipeResponse.class));

        assertNotNull(service.retirerChef(1L));
        assertNull(e.getChef());
        verify(equipeRepository).save(e);
        verifyNoInteractions(userRepository); // save never called because chef was null
    }
}

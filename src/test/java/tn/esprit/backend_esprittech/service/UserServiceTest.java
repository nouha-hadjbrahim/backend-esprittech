package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.esprit.backend_esprittech.dto.request.CreateUserRequest;
import tn.esprit.backend_esprittech.dto.request.UpdateUserRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.EmailAlreadyExistsException;
import tn.esprit.backend_esprittech.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .nom("Test").prenom("User")
                .email("test@esprit.tn")
                .password("encoded")
                .role(Role.ROLE_ETUDIANT)
                .typeUtilisateur(TypeUtilisateur.ETUDIANT)
                .enabled(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- findById ----

    @Test
    void findById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertEquals(user, result);
    }

    @Test
    void findById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.findById(99L));
    }

    // ---- findByEmail ----

    @Test
    void findByEmail_success() {
        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));

        User result = userService.findByEmail("test@esprit.tn");

        assertEquals(user, result);
    }

    @Test
    void findByEmail_notFound_throws() {
        when(userRepository.findByEmail("unknown@esprit.tn")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> userService.findByEmail("unknown@esprit.tn"));
    }

    // ---- getCurrentUser ----

    @Test
    void getCurrentUser_nullAuthentication_throws() {
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_notAuthenticated_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_stringPrincipal_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(NoSuchElementException.class, () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_withUserPrincipal_returnsUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = userService.getCurrentUser();

        assertEquals(user, result);
    }

    @Test
    void getCurrentUser_otherPrincipal_fallsBackToFindByEmail() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(42);
        when(auth.getName()).thenReturn("test@esprit.tn");
        when(userRepository.findByEmail("test@esprit.tn")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = userService.getCurrentUser();

        assertEquals(user, result);
    }

    // ---- create ----

    @Test
    void create_success_encodesPasswordAndDerivesType() {
        var request = new CreateUserRequest("Khaldi", "Ines", "ines@esprit.tn",
                "Passw0rd1", Role.ROLE_ENSEIGNANT, true);
        when(userRepository.existsByEmail("ines@esprit.tn")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.create(request);

        assertEquals("hashed", result.getPassword());
        assertEquals(Role.ROLE_ENSEIGNANT, result.getRole());
        assertEquals(TypeUtilisateur.ENSEIGNANT, result.getTypeUtilisateur());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_studentRole_derivesEtudiantType() {
        var request = new CreateUserRequest("Trabelsi", "Sarra", "sarra@esprit.tn",
                "Passw0rd1", Role.ROLE_ETUDIANT, true);
        when(userRepository.existsByEmail("sarra@esprit.tn")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.create(request);

        assertEquals(TypeUtilisateur.ETUDIANT, result.getTypeUtilisateur());
    }

    @Test
    void create_emailExists_throws() {
        var request = new CreateUserRequest("X", "Y", "dup@esprit.tn",
                "Passw0rd1", Role.ROLE_ETUDIANT, true);
        when(userRepository.existsByEmail("dup@esprit.tn")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.create(request));
        verify(userRepository, never()).save(any());
    }

    // ---- list ----

    @Test
    void list_withSearch_usesSearchQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.search("test", pageable)).thenReturn(page);

        Page<User> result = userService.list("test", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).search("test", pageable);
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void list_withoutSearch_usesFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.list("  ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    // ---- listEligible ----

    @Test
    void listEligible_typeMember_callsSearchEligibleMembers() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchEligibleMembers("test", roles, pageable)).thenReturn(page);

        Page<User> result = userService.listEligible("test", 1L, "MEMBER", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchEligibleMembers("test", roles, pageable);
        verify(userRepository, never()).searchEligible(any(), any(), any(), any());
    }

    @Test
    void listEligible_typeChefWithEquipeId_callsSearchEligible() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchEligible("test", 1L, roles, pageable)).thenReturn(page);

        Page<User> result = userService.listEligible("test", 1L, "CHEF", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchEligible("test", 1L, roles, pageable);
        verify(userRepository, never()).searchEligibleMembers(any(), any(), any());
    }

    @Test
    void listEligible_typeChefWithoutEquipeId_fallsBackToEligibleMembers() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchEligibleMembers("test", roles, pageable)).thenReturn(page);

        Page<User> result = userService.listEligible("test", null, "CHEF", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchEligibleMembers("test", roles, pageable);
        verify(userRepository, never()).searchEligible(any(), any(), any(), any());
    }

    @Test
    void listEligible_trimsSearchQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchEligibleMembers("in", roles, pageable)).thenReturn(page);

        Page<User> result = userService.listEligible("  in  ", null, "MEMBER", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchEligibleMembers("in", roles, pageable);
    }

    @Test
    void listEligible_nullSearch_passesEmptyString() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Role> roles = List.of(Role.ROLE_ENSEIGNANT);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.searchEligibleMembers("", roles, pageable)).thenReturn(page);

        Page<User> result = userService.listEligible(null, null, "MEMBER", pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository).searchEligibleMembers("", roles, pageable);
    }

    // ---- update ----

    @Test
    void update_success_updatesFields() {
        var request = new UpdateUserRequest("Nouveau", "Nom", "test@esprit.tn",
                Role.ROLE_CHEF_EQUIPE, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1L, request);

        assertEquals("Nouveau", result.getNom());
        assertEquals(Role.ROLE_CHEF_EQUIPE, result.getRole());
        assertFalse(result.isEnabled());
    }

    @Test
    void update_emailChangedAndTaken_throws() {
        var request = new UpdateUserRequest("Test", "User", "autre@esprit.tn",
                Role.ROLE_ETUDIANT, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("autre@esprit.tn")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.update(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_emailChangedNotTaken_proceeds() {
        var request = new UpdateUserRequest("Nouveau", "Nom", "libre@esprit.tn",
                Role.ROLE_CHEF_EQUIPE, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("libre@esprit.tn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1L, request);

        assertEquals("libre@esprit.tn", result.getEmail());
        assertEquals("Nouveau", result.getNom());
        assertEquals(Role.ROLE_CHEF_EQUIPE, result.getRole());
        assertFalse(result.isEnabled());
        verify(userRepository).save(any(User.class));
    }

    // ---- delete ----

    @Test
    void delete_success() {
        User current = User.builder().id(2L).nom("Admin").prenom("A")
                .email("admin@esprit.tn").role(Role.ROLE_ADMIN)
                .typeUtilisateur(TypeUtilisateur.ENSEIGNANT).enabled(true).build();
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(current);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void delete_self_throws() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.delete(1L));
        verify(userRepository, never()).delete(any());
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsUserFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var result = userService.getOldValues(1L);

        assertEquals(user.getNom(), result.get("nom"));
        assertEquals(user.getPrenom(), result.get("prenom"));
        assertEquals(user.getEmail(), result.get("email"));
        assertEquals(user.getRole(), result.get("role"));
    }
}

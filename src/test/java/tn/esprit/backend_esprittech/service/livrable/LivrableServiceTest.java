package tn.esprit.backend_esprittech.service.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivrableServiceTest {

    @Mock
    private LivrableRepository livrableRepository;

    @Mock
    private SujetProjetRepository sujetProjetRepository;

    @Mock
    private UserService userService;

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private LivrableService livrableService;

    private User encadrant;
    private User admin;
    private User ci;
    private User otherTeacher;
    private SujetProjet projet;

    @BeforeEach
    void setUp() {
        encadrant = new User();
        encadrant.setId(7L);
        encadrant.setPrenom("Ali");
        encadrant.setNom("Mansour");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        admin = new User();
        admin.setId(1L);
        admin.setPrenom("Admin");
        admin.setNom("Root");
        admin.setRole(Role.ROLE_ADMIN);

        ci = new User();
        ci.setId(9L);
        ci.setPrenom("Comite");
        ci.setNom("Industrie");
        ci.setRole(Role.ROLE_CI);

        otherTeacher = new User();
        otherTeacher.setId(99L);
        otherTeacher.setPrenom("Autre");
        otherTeacher.setNom("Prof");
        otherTeacher.setRole(Role.ROLE_ENSEIGNANT);

        projet = new SujetProjet();
        projet.setId(42L);
        projet.setTitre("Plateforme IoT");
        projet.setEncadrantId(7L);
        projet.setStatut(StatutSujet.REALISATION_EN_COURS);
    }

    @Test
    void upload_validFile_shouldStoreInMinioAndPersistMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport final.pdf",
                "application/pdf",
                "content".getBytes()
        );
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(storageService.upload(anyString(), any())).thenReturn(
                new MinioStorageService.StoredObject("livrables/42/stored.pdf", "application/pdf", file.getSize())
        );
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> {
            Livrable livrable = invocation.getArgument(0);
            livrable.setId(100L);
            return livrable;
        });

        var response = livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", "Version finale", file);

        assertEquals(100L, response.id());
        assertEquals("Rapport", response.nom());
        assertEquals("rapport final.pdf", response.originalFileName());
        ArgumentCaptor<String> objectNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(objectNameCaptor.capture(), any());
        assertTrue(objectNameCaptor.getValue().startsWith("livrables/42/"));
        assertTrue(objectNameCaptor.getValue().endsWith("-rapport_final.pdf"));
    }

    @Test
    void upload_afterTermination_shouldStillBeAllowed() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documentation.pdf",
                "application/pdf",
                "content".getBytes()
        );
        projet.setStatut(StatutSujet.REALISATION_TERMINEE);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(storageService.upload(anyString(), any())).thenReturn(
                new MinioStorageService.StoredObject("livrables/42/documentation.pdf", "application/pdf", file.getSize())
        );
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = livrableService.upload(42L, TypeLivrable.DOCUMENTATION, "Documentation", null, file);

        assertEquals("Documentation", response.nom());
    }

    @Test
    void upload_invalidExtension_shouldRejectFileBeforeStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "install.exe",
                "application/octet-stream",
                "binary".getBytes()
        );
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.upload(42L, TypeLivrable.AUTRE, "Executable", null, file));

        assertEquals("Format non autorise. Formats acceptes: pdf, doc, docx, png, jpg, jpeg, txt, md, html, json, xml, lcov.", ex.getMessage());
    }

    @Test
    void addLink_invalidUrl_shouldRejectRequest() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        LivrableLinkRequest request = new LivrableLinkRequest(
                TypeLivrable.LIEN_GIT,
                "Depot Git",
                null,
                "ftp://example.com/repo"
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.addLink(42L, request));

        assertEquals("Le lien externe doit etre une URL http(s) valide.", ex.getMessage());
    }

    @Test
    void addLink_validUrl_shouldPersistExternalLink() {
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> {
            Livrable livrable = invocation.getArgument(0);
            livrable.setId(101L);
            return livrable;
        });
        LivrableLinkRequest request = new LivrableLinkRequest(
                TypeLivrable.LIEN_GIT,
                "Depot Git",
                "Code source",
                "https://github.com/esprit/demo"
        );

        var response = livrableService.addLink(42L, request);

        assertEquals(101L, response.id());
        assertEquals("https://github.com/esprit/demo", response.lienExterne());
        assertEquals(TypeLivrable.LIEN_GIT, response.typeLivrable());
    }

    @Test
    void upload_emptyFile_shouldRejectFile() {
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", new byte[0]);
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));

        assertEquals("Le fichier est obligatoire.", ex.getMessage());
        verify(storageService, never()).upload(anyString(), any());
    }

    @Test
    void upload_invalidMimeType_shouldRejectFile() {
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/x-msdownload", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));

        assertEquals("Type MIME non autorise.", ex.getMessage());
    }

    @Test
    void upload_invalidFileName_shouldRejectPathTraversal() {
        MockMultipartFile file = new MockMultipartFile("file", "../rapport.pdf", "application/pdf", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));

        assertEquals("Nom de fichier invalide.", ex.getMessage());
    }

    @Test
    void upload_nonOwnerTeacher_shouldRejectDeposit() {
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(otherTeacher);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));
    }

    @Test
    void upload_forbiddenProjectStatus_shouldRejectOwnerDeposit() {
        projet.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));

        assertEquals("Les livrables peuvent etre deposes pendant la realisation ou apres terminaison.", ex.getMessage());
    }

    @Test
    void upload_adminShouldBypassOwnerAndStatusChecks() {
        projet.setStatut(StatutSujet.SOUMIS_EN_VALIDATION);
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(admin);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(storageService.upload(anyString(), any())).thenReturn(
                new MinioStorageService.StoredObject("livrables/42/rapport.pdf", "application/pdf", file.getSize())
        );
        when(livrableRepository.save(any(Livrable.class))).thenAnswer(invocation -> {
            Livrable livrable = invocation.getArgument(0);
            livrable.setId(102L);
            return livrable;
        });

        var response = livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file);

        assertEquals(102L, response.id());
    }

    @Test
    void upload_unknownProject_shouldThrowResourceNotFound() {
        MockMultipartFile file = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "data".getBytes());
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> livrableService.upload(42L, TypeLivrable.RAPPORT, "Rapport", null, file));

        assertEquals("Projet introuvable avec id: 42", ex.getMessage());
    }

    @Test
    void findByProjet_owner_shouldReturnActiveLivrables() {
        Livrable livrable = existingLivrable();
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(List.of(livrable));

        var result = livrableService.findByProjet(42L);

        assertEquals(1, result.size());
        assertEquals("Rapport", result.get(0).nom());
    }

    @Test
    void findByProjet_ci_shouldBeAllowedToRead() {
        when(userService.getCurrentUser()).thenReturn(ci);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(List.of());

        var result = livrableService.findByProjet(42L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByProjet_nonOwnerTeacher_shouldRejectRead() {
        when(userService.getCurrentUser()).thenReturn(otherTeacher);
        when(sujetProjetRepository.findById(42L)).thenReturn(Optional.of(projet));

        assertThrows(AccessDeniedException.class, () -> livrableService.findByProjet(42L));
    }

    @Test
    void findAllActiveForAdmin_admin_shouldReturnLivrables() {
        when(userService.getCurrentUser()).thenReturn(admin);
        when(livrableRepository.findByActifTrueOrderByDateDepotDesc()).thenReturn(List.of(existingLivrable()));

        var result = livrableService.findAllActiveForAdmin();

        assertEquals(1, result.size());
        verify(livrableRepository).findByActifTrueOrderByDateDepotDesc();
    }

    @Test
    void findAllActiveForAdmin_nonAdmin_shouldReject() {
        when(userService.getCurrentUser()).thenReturn(encadrant);

        assertThrows(AccessDeniedException.class, () -> livrableService.findAllActiveForAdmin());
    }

    @Test
    void findById_activeLivrableAndOwner_shouldReturnResponse() {
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(existingLivrable()));
        when(userService.getCurrentUser()).thenReturn(encadrant);

        var result = livrableService.findById(100L);

        assertEquals(100L, result.id());
    }

    @Test
    void findById_inactiveLivrable_shouldThrowResourceNotFound() {
        Livrable livrable = existingLivrable();
        livrable.setActif(false);
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> livrableService.findById(100L));

        assertEquals("Livrable introuvable avec id: 100", ex.getMessage());
    }

    @Test
    void update_validRequest_shouldPersistUpdatedMetadata() {
        Livrable livrable = existingLivrable();
        LivrableUpdateRequest request = new LivrableUpdateRequest(
                TypeLivrable.DOCUMENTATION,
                "Documentation",
                "Guide",
                "https://example.com/doc",
                false
        );
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(livrableRepository.save(livrable)).thenReturn(livrable);

        var result = livrableService.update(100L, request);

        assertEquals("Documentation", result.nom());
        assertEquals(false, result.actif());
        assertEquals("https://example.com/doc", result.lienExterne());
    }

    @Test
    void update_invalidUrl_shouldReject() {
        Livrable livrable = existingLivrable();
        LivrableUpdateRequest request = new LivrableUpdateRequest(TypeLivrable.RAPPORT, "Rapport", null, "notaurl", true);
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));
        when(userService.getCurrentUser()).thenReturn(encadrant);

        BusinessException ex = assertThrows(BusinessException.class, () -> livrableService.update(100L, request));

        assertEquals("Le lien externe doit etre une URL http(s) valide.", ex.getMessage());
    }

    @Test
    void delete_activeLivrable_shouldDeactivate() {
        Livrable livrable = existingLivrable();
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));
        when(userService.getCurrentUser()).thenReturn(encadrant);

        livrableService.delete(100L);

        assertEquals(false, livrable.getActif());
        verify(livrableRepository).save(livrable);
    }

    @Test
    void download_fileLivrable_shouldReturnStorageObject() {
        Livrable livrable = existingLivrable();
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));
        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(storageService.download("livrables/42/rapport.pdf")).thenReturn(
                new MinioStorageService.DownloadedObject(new ByteArrayInputStream("abc".getBytes()), "application/pdf", 3L)
        );

        var result = livrableService.download(100L);

        assertEquals("rapport.pdf", result.filename());
        assertEquals("application/pdf", result.contentType());
        assertEquals(3L, result.size());
    }

    @Test
    void download_externalLink_shouldRejectBecauseNoFile() {
        Livrable livrable = existingLivrable();
        livrable.setObjectName(null);
        livrable.setLienExterne("https://github.com/esprit/demo");
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(livrable));
        when(userService.getCurrentUser()).thenReturn(encadrant);

        BusinessException ex = assertThrows(BusinessException.class, () -> livrableService.download(100L));

        assertEquals("Ce livrable est un lien externe et ne contient pas de fichier.", ex.getMessage());
    }

    @Test
    void hasActiveLivrables_shouldDelegateToRepository() {
        when(livrableRepository.existsBySujetProjetIdAndActifTrue(42L)).thenReturn(true);

        assertTrue(livrableService.hasActiveLivrables(42L));
    }

    // ---- getOldValues ----

    @Test
    void getOldValues_returnsLivrableFields() {
        Livrable l = existingLivrable();
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(l));

        var result = livrableService.getOldValues(100L);

        assertEquals(l.getNom(), result.get("nom"));
        assertEquals(l.getTypeLivrable(), result.get("typeLivrable"));
        assertEquals(l.getActif(), result.get("actif"));
    }

    private Livrable existingLivrable() {
        Livrable livrable = new Livrable();
        livrable.setId(100L);
        livrable.setSujetProjet(projet);
        livrable.setTypeLivrable(TypeLivrable.RAPPORT);
        livrable.setNom("Rapport");
        livrable.setDescription("Version finale");
        livrable.setOriginalFileName("rapport.pdf");
        livrable.setObjectName("livrables/42/rapport.pdf");
        livrable.setContentType("application/pdf");
        livrable.setSize(128L);
        livrable.setDeposant(encadrant);
        livrable.setDateDepot(LocalDateTime.now());
        livrable.setActif(true);
        return livrable;
    }

    // ---- getNomById ----

    @Test
    void getNomById_found_returnsNom() {
        when(livrableRepository.findById(100L)).thenReturn(Optional.of(existingLivrable()));

        assertEquals("Rapport", livrableService.getNomById(100L));
    }

    @Test
    void getNomById_notFound_returnsInconnu() {
        when(livrableRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals("Inconnu", livrableService.getNomById(99L));
    }
}

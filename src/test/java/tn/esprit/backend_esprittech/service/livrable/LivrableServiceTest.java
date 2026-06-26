package tn.esprit.backend_esprittech.service.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.entity.sujetprojet.enums.StatutSujet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetProjetRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private SujetProjet projet;

    @BeforeEach
    void setUp() {
        encadrant = new User();
        encadrant.setId(7L);
        encadrant.setPrenom("Ali");
        encadrant.setNom("Mansour");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

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

        assertEquals("Format non autorise. Formats acceptes: pdf, doc, docx, png, jpg, jpeg, txt.", ex.getMessage());
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
}

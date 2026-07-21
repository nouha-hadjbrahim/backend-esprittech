package tn.esprit.backend_esprittech.service.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.backend_esprittech.dto.livrable.LivrableLinkRequest;
import tn.esprit.backend_esprittech.dto.livrable.LivrableUpdateRequest;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LivrableCatalogueServiceTest {

    @Mock
    private LivrableCatalogueRepository livrableRepository;

    @Mock
    private LivrableRepository sujetLivrableRepository;

    @Mock
    private ProjetCatalogueRepository projetRepository;

    @Mock
    private UserService userService;

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private LivrableCatalogueService service;

    private User deposant;
    private ProjetCatalogue projet;
    private SujetProjet sujet;

    @BeforeEach
    void setUp() {
        deposant = new User();
        deposant.setId(7L);
        deposant.setPrenom("Ali");
        deposant.setNom("Mansour");

        sujet = new SujetProjet();
        sujet.setId(42L);
        sujet.setTitre("Sujet IA");

        projet = ProjetCatalogue.builder()
                .id(100L)
                .sujetId(42L)
                .titre("Projet IA")
                .statut(StatutProjet.VALIDE)
                .build();
    }

    @Test
    void findPublishedForPublicCatalogue_convertedProjectReturnsOriginalSujetDeliverables() {
        Livrable inherited = sujetLivrable(10L, "Rapport sujet", "livrables/42/rapport.pdf",
                LocalDateTime.of(2026, 3, 1, 10, 0));
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of());
        when(sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(List.of(inherited));

        var result = service.findPublishedForPublicCatalogue(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).projetId());
        assertEquals(10L, result.get(0).id());
        assertEquals("livrables/42/rapport.pdf", result.get(0).objectName());
        assertTrue(result.get(0).fromSujet());
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void findPublishedForPublicCatalogue_returnsDirectAndInheritedDeliverablesTogether() {
        LivrableCatalogue direct = catalogueLivrable(20L, "Doc projet", "livrables-catalogue/100/doc.pdf",
                LocalDateTime.of(2026, 4, 1, 10, 0));
        Livrable inherited = sujetLivrable(10L, "Rapport sujet", "livrables/42/rapport.pdf",
                LocalDateTime.of(2026, 3, 1, 10, 0));
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of(direct));
        when(sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(List.of(inherited));

        var result = service.findPublishedForPublicCatalogue(100L);

        assertEquals(2, result.size());
        assertFalse(result.get(0).fromSujet());
        assertTrue(result.get(1).fromSujet());
        assertEquals("livrables-catalogue/100/doc.pdf", result.get(0).objectName());
        assertEquals("livrables/42/rapport.pdf", result.get(1).objectName());
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void findPublishedForPublicCatalogue_deduplicatesByObjectNameAndKeepsCatalogueMetadata() {
        LivrableCatalogue direct = catalogueLivrable(20L, "Doc projet", "shared/object.pdf",
                LocalDateTime.of(2026, 4, 1, 10, 0));
        Livrable inherited = sujetLivrable(10L, "Doc sujet", "shared/object.pdf",
                LocalDateTime.of(2026, 5, 1, 10, 0));
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of(direct));
        when(sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L)).thenReturn(List.of(inherited));

        var result = service.findPublishedForPublicCatalogue(100L);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
        assertEquals("Doc projet", result.get(0).nom());
        assertFalse(result.get(0).fromSujet());
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void findPublishedForPublicCatalogue_noDeliverablesReturnsEmptyList() {
        projet.setSujetId(null);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of());

        var result = service.findPublishedForPublicCatalogue(100L);

        assertTrue(result.isEmpty());
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void findPublishedForPublicCatalogue_unpublishedProjectReturnsControlledNotFound() {
        projet.setStatut(StatutProjet.SOUMIS_EN_VALIDATION);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));

        assertThrows(ResourceNotFoundException.class, () -> service.findPublishedForPublicCatalogue(100L));

        verify(livrableRepository, never()).findByProjetIdAndActifTrueOrderByDateDepotDesc(any());
        verify(sujetLivrableRepository, never()).findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(any());
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void downloadPublished_fromSujetDownloadsInheritedObjectWithoutCopying() {
        Livrable inherited = sujetLivrable(10L, "Rapport sujet", "livrables/42/rapport.pdf",
                LocalDateTime.of(2026, 3, 1, 10, 0));
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(sujetLivrableRepository.findById(10L)).thenReturn(Optional.of(inherited));
        when(storageService.download("livrables/42/rapport.pdf")).thenReturn(
                new MinioStorageService.DownloadedObject(new ByteArrayInputStream("abc".getBytes()), "application/pdf", 3L)
        );

        var result = service.downloadPublished(100L, 10L, true);

        assertEquals("rapport.pdf", result.filename());
        assertEquals("application/pdf", result.contentType());
        assertEquals(3L, result.size());
        verify(storageService).download("livrables/42/rapport.pdf");
        verifyNoCopyOrMetadataRewrite();
    }

    @Test
    void downloadPublished_fromSujetRejectsUnrelatedSujetDeliverable() {
        SujetProjet otherSujet = new SujetProjet();
        otherSujet.setId(999L);
        otherSujet.setTitre("Autre sujet");
        Livrable unrelated = sujetLivrable(10L, "Autre rapport", "livrables/999/rapport.pdf",
                LocalDateTime.of(2026, 3, 1, 10, 0));
        unrelated.setSujetProjet(otherSujet);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(sujetLivrableRepository.findById(10L)).thenReturn(Optional.of(unrelated));

        assertThrows(ResourceNotFoundException.class, () -> service.downloadPublished(100L, 10L, true));

        verify(storageService, never()).download(anyString());
        verifyNoCopyOrMetadataRewrite();
    }

    // ---- validateFile ----

    @Test
    void validateFile_nullOrEmpty_shouldReject() {
        var fichierVide = new MockMultipartFile("f", "rapport.pdf", "application/pdf", new byte[0]);
        BusinessException empty = assertThrows(BusinessException.class,
                () -> service.validateFile(fichierVide));

        assertEquals("Le fichier est obligatoire.", empty.getMessage());
        assertEquals("Le fichier est obligatoire.",
                assertThrows(BusinessException.class, () -> service.validateFile(null)).getMessage());
    }

    @Test
    void validateFile_pathTraversalInName_shouldReject() {
        var file = new MockMultipartFile("f", "../../etc/passwd.pdf", "application/pdf", "x".getBytes());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.validateFile(file));

        assertEquals("Nom de fichier invalide.", ex.getMessage());
    }

    @Test
    void validateFile_disallowedExtension_shouldReject() {
        var file = new MockMultipartFile("f", "script.exe", "application/pdf", "x".getBytes());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.validateFile(file));

        assertTrue(ex.getMessage().startsWith("Format non autorise."));
    }

    @Test
    void validateFile_disallowedContentType_shouldReject() {
        var file = new MockMultipartFile("f", "rapport.pdf", "application/x-msdownload", "x".getBytes());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.validateFile(file));

        assertEquals("Type MIME non autorise.", ex.getMessage());
    }

    @Test
    void validateFile_allowedPdf_shouldPass() {
        var file = new MockMultipartFile("f", "rapport.pdf", "application/pdf", "x".getBytes());

        assertDoesNotThrow(() -> service.validateFile(file));
    }

    // ---- addLink : validation du lien externe ----

    @Test
    void addLink_nonHttpUrl_shouldReject() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        var request = new LivrableLinkRequest(TypeLivrable.DOCUMENTATION, "Depot Git", null, "ftp://serveur/depot");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addLink(100L, request));

        assertEquals("Le lien externe doit etre une URL http(s) valide.", ex.getMessage());
        verify(livrableRepository, never()).save(any());
    }

    @Test
    void addLink_blankName_shouldReject() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        var request = new LivrableLinkRequest(TypeLivrable.DOCUMENTATION, "  ", null, "https://git.example.tn/depot");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.addLink(100L, request));

        assertEquals("Le nom du livrable est obligatoire.", ex.getMessage());
        verify(livrableRepository, never()).save(any());
    }

    @Test
    void addLink_validHttpsUrl_shouldPersistLink() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.save(any(LivrableCatalogue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var request = new LivrableLinkRequest(
                TypeLivrable.DOCUMENTATION, "Depot Git", "Sources", "https://git.example.tn/depot");

        var response = service.addLink(100L, request);

        assertEquals("Depot Git", response.nom());
        assertEquals("https://git.example.tn/depot", response.lienExterne());
        verify(livrableRepository).save(any(LivrableCatalogue.class));
    }

    // ---- Controle d'acces au depot ----

    @Test
    void addLink_studentRole_shouldBeDenied() {
        deposant.setRole(Role.ROLE_ETUDIANT);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        var request = new LivrableLinkRequest(TypeLivrable.DOCUMENTATION, "Depot", null, "https://git.example.tn/d");

        assertThrows(AccessDeniedException.class, () -> service.addLink(100L, request));
        verify(livrableRepository, never()).save(any());
    }

    @Test
    void addLink_unknownProject_shouldReportNotFound() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(999L)).thenReturn(Optional.empty());
        var request = new LivrableLinkRequest(TypeLivrable.DOCUMENTATION, "Depot", null, "https://git.example.tn/d");

        assertThrows(ResourceNotFoundException.class, () -> service.addLink(999L, request));
    }

    // ---- upload ----

    @Test
    void upload_shouldStoreFileAndPersistMetadata() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(storageService.upload(anyString(), any()))
                .thenReturn(new MinioStorageService.StoredObject("livrables-catalogue/100/x-rapport.pdf",
                        "application/pdf", 42L));
        when(livrableRepository.save(any(LivrableCatalogue.class))).thenAnswer(i -> i.getArgument(0));
        var file = new MockMultipartFile("f", "rapport.pdf", "application/pdf", "contenu".getBytes());

        var response = service.upload(100L, TypeLivrable.RAPPORT, " Rapport final ", " Version 1 ", file);

        assertEquals("Rapport final", response.nom());
        assertEquals("rapport.pdf", response.originalFileName());
        assertEquals("livrables-catalogue/100/x-rapport.pdf", response.objectName());
        assertEquals(42L, response.size());
    }

    @Test
    void upload_blankName_shouldRejectBeforeStoring() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        var file = new MockMultipartFile("f", "rapport.pdf", "application/pdf", "x".getBytes());

        assertThrows(BusinessException.class,
                () -> service.upload(100L, TypeLivrable.RAPPORT, "  ", null, file));

        verify(storageService, never()).upload(anyString(), any());
    }

    @Test
    void upload_onUnvalidatedProject_shouldBeRejected() {
        projet.setStatut(StatutProjet.SOUMIS_EN_VALIDATION);
        deposant.setRole(Role.ROLE_ENSEIGNANT);
        projet.setEncadrant(deposant);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        var file = new MockMultipartFile("f", "rapport.pdf", "application/pdf", "x".getBytes());

        assertThrows(BusinessException.class,
                () -> service.upload(100L, TypeLivrable.RAPPORT, "Rapport", null, file));
    }

    // ---- findByProjet / findById / hasActiveLivrables ----

    @Test
    void findByProjet_shouldPreferCatalogueDeliverables() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L))
                .thenReturn(List.of(catalogueLivrable(20L, "Doc", "livrables-catalogue/100/doc.pdf",
                        LocalDateTime.of(2026, 4, 1, 10, 0))));

        var result = service.findByProjet(100L);

        assertEquals(1, result.size());
        assertEquals("Doc", result.get(0).nom());
        verify(sujetLivrableRepository, never()).findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(any());
    }

    @Test
    void findByProjet_withoutCatalogueDeliverables_shouldFallBackToSujet() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of());
        when(sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L))
                .thenReturn(List.of(sujetLivrable(10L, "Rapport sujet", "livrables/42/rapport.pdf",
                        LocalDateTime.of(2026, 3, 1, 10, 0))));

        var result = service.findByProjet(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).projetId());
    }

    @Test
    void findByProjet_withoutOriginSujet_shouldReturnEmptyList() {
        projet.setSujetId(null);
        deposant.setRole(Role.ROLE_ADMIN);
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(100L)).thenReturn(List.of());

        assertTrue(service.findByProjet(100L).isEmpty());
    }

    @Test
    void findById_inactiveDeliverable_shouldReportNotFound() {
        LivrableCatalogue inactif = catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now());
        inactif.setActif(false);
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(inactif));

        assertThrows(ResourceNotFoundException.class, () -> service.findById(20L));
    }

    @Test
    void hasActiveLivrables_shouldFallBackToOriginSujet() {
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.existsByProjetIdAndActifTrue(100L)).thenReturn(false);
        when(sujetLivrableRepository.existsBySujetProjetIdAndActifTrue(42L)).thenReturn(true);

        assertTrue(service.hasActiveLivrables(100L));
    }

    @Test
    void hasActiveLivrables_catalogueDeliverablePresent_shouldReturnTrueImmediately() {
        when(projetRepository.findById(100L)).thenReturn(Optional.of(projet));
        when(livrableRepository.existsByProjetIdAndActifTrue(100L)).thenReturn(true);

        assertTrue(service.hasActiveLivrables(100L));
        verify(sujetLivrableRepository, never()).existsBySujetProjetIdAndActifTrue(any());
    }

    // ---- update / delete ----

    @Test
    void update_shouldApplyNewValues() {
        deposant.setRole(Role.ROLE_ADMIN);
        LivrableCatalogue existing = catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now());
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(livrableRepository.save(any(LivrableCatalogue.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.update(20L, new LivrableUpdateRequest(
                TypeLivrable.DOCUMENTATION, " Doc revisee ", " Nouvelle version ",
                "https://git.example.tn/depot", true));

        assertEquals("Doc revisee", response.nom());
        assertEquals("Nouvelle version", response.description());
        assertEquals("https://git.example.tn/depot", response.lienExterne());
    }

    @Test
    void update_invalidExternalLink_shouldReject() {
        deposant.setRole(Role.ROLE_ADMIN);
        when(livrableRepository.findById(20L))
                .thenReturn(Optional.of(catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now())));
        when(userService.getCurrentUser()).thenReturn(deposant);
        var request = new LivrableUpdateRequest(TypeLivrable.DOCUMENTATION, "Doc", null, "ftp://x/y", true);

        assertThrows(BusinessException.class, () -> service.update(20L, request));
        verify(livrableRepository, never()).save(any());
    }

    @Test
    void update_byUnrelatedTeacher_shouldBeDenied() {
        deposant.setRole(Role.ROLE_ENSEIGNANT);
        projet.setEncadrant(autreEnseignant());
        when(livrableRepository.findById(20L))
                .thenReturn(Optional.of(catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now())));
        when(userService.getCurrentUser()).thenReturn(deposant);
        var request = new LivrableUpdateRequest(TypeLivrable.DOCUMENTATION, "Doc", null, null, true);

        assertThrows(AccessDeniedException.class, () -> service.update(20L, request));
    }

    @Test
    void delete_shouldSoftDeleteDeliverable() {
        deposant.setRole(Role.ROLE_ADMIN);
        LivrableCatalogue existing = catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now());
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userService.getCurrentUser()).thenReturn(deposant);

        service.delete(20L);

        assertFalse(existing.getActif());
        verify(livrableRepository).save(existing);
    }

    // ---- download ----

    @Test
    void download_shouldReturnStoredFile() {
        deposant.setRole(Role.ROLE_ADMIN);
        LivrableCatalogue existing = catalogueLivrable(20L, "Doc", "livrables-catalogue/100/doc.pdf",
                LocalDateTime.now());
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(storageService.download("livrables-catalogue/100/doc.pdf")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("x".getBytes()), "application/pdf", 1L));

        var download = service.download(20L);

        assertEquals("doc.pdf", download.filename());
        assertEquals("application/pdf", download.contentType());
    }

    @Test
    void download_externalLink_shouldReject() {
        deposant.setRole(Role.ROLE_ADMIN);
        LivrableCatalogue lien = catalogueLivrable(20L, "Depot", null, LocalDateTime.now());
        lien.setLienExterne("https://git.example.tn/depot");
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(lien));
        when(userService.getCurrentUser()).thenReturn(deposant);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.download(20L));

        assertEquals("Ce livrable est un lien externe et ne contient pas de fichier.", ex.getMessage());
        verify(storageService, never()).download(anyString());
    }

    @Test
    void download_byStudentOnPublishedProject_shouldBeAllowed() {
        deposant.setRole(Role.ROLE_ETUDIANT);
        LivrableCatalogue existing = catalogueLivrable(20L, "Doc", "livrables-catalogue/100/doc.pdf",
                LocalDateTime.now());
        when(livrableRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userService.getCurrentUser()).thenReturn(deposant);
        when(storageService.download("livrables-catalogue/100/doc.pdf")).thenReturn(
                new MinioStorageService.DownloadedObject(
                        new ByteArrayInputStream("x".getBytes()), "application/pdf", 1L));

        assertNotNull(service.download(20L));
    }

    @Test
    void findById_onUnpublishedProjectByStudent_shouldBeDenied() {
        projet.setStatut(StatutProjet.SOUMIS_EN_VALIDATION);
        deposant.setRole(Role.ROLE_ETUDIANT);
        when(livrableRepository.findById(20L))
                .thenReturn(Optional.of(catalogueLivrable(20L, "Doc", "obj", LocalDateTime.now())));
        when(userService.getCurrentUser()).thenReturn(deposant);

        assertThrows(AccessDeniedException.class, () -> service.findById(20L));
    }

    private User autreEnseignant() {
        User autre = new User();
        autre.setId(999L);
        autre.setRole(Role.ROLE_ENSEIGNANT);
        return autre;
    }

    private Livrable sujetLivrable(Long id, String nom, String objectName, LocalDateTime dateDepot) {
        Livrable livrable = new Livrable();
        livrable.setId(id);
        livrable.setSujetProjet(sujet);
        livrable.setTypeLivrable(TypeLivrable.RAPPORT);
        livrable.setNom(nom);
        livrable.setDescription("Version finale");
        livrable.setOriginalFileName("rapport.pdf");
        livrable.setObjectName(objectName);
        livrable.setContentType("application/pdf");
        livrable.setSize(128L);
        livrable.setDeposant(deposant);
        livrable.setDateDepot(dateDepot);
        livrable.setActif(true);
        return livrable;
    }

    private LivrableCatalogue catalogueLivrable(Long id, String nom, String objectName, LocalDateTime dateDepot) {
        LivrableCatalogue livrable = new LivrableCatalogue();
        livrable.setId(id);
        livrable.setProjet(projet);
        livrable.setTypeLivrable(TypeLivrable.DOCUMENTATION);
        livrable.setNom(nom);
        livrable.setDescription("Documentation");
        livrable.setOriginalFileName("doc.pdf");
        livrable.setObjectName(objectName);
        livrable.setContentType("application/pdf");
        livrable.setSize(256L);
        livrable.setDeposant(deposant);
        livrable.setDateDepot(dateDepot);
        livrable.setActif(true);
        return livrable;
    }

    private void verifyNoCopyOrMetadataRewrite() {
        verify(storageService, never()).upload(anyString(), any());
        verify(livrableRepository, never()).save(any());
        verify(sujetLivrableRepository, never()).save(any());
    }
}

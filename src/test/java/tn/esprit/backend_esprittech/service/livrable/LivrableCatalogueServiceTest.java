package tn.esprit.backend_esprittech.service.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

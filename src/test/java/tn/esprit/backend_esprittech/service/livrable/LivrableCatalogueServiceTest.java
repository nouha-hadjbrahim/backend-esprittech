package tn.esprit.backend_esprittech.service.livrable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.livrable.LivrableCatalogueResponse;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.entity.livrable.Livrable;
import tn.esprit.backend_esprittech.entity.livrable.LivrableCatalogue;
import tn.esprit.backend_esprittech.entity.livrable.TypeLivrable;
import tn.esprit.backend_esprittech.entity.projetcatalogue.ProjetCatalogue;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetProjet;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.enums.StatutProjet;
import tn.esprit.backend_esprittech.repository.livrable.LivrableCatalogueRepository;
import tn.esprit.backend_esprittech.repository.livrable.LivrableRepository;
import tn.esprit.backend_esprittech.repository.projetcatalogue.ProjetCatalogueRepository;
import tn.esprit.backend_esprittech.service.UserService;
import tn.esprit.backend_esprittech.service.storage.MinioStorageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private LivrableCatalogueService livrableCatalogueService;

    private User encadrant;
    private ProjetCatalogue projet;

    @BeforeEach
    void setUp() {
        encadrant = new User();
        encadrant.setId(7L);
        encadrant.setPrenom("Ali");
        encadrant.setNom("Mansour");
        encadrant.setRole(Role.ROLE_ENSEIGNANT);

        projet = new ProjetCatalogue();
        projet.setId(55L);
        projet.setTitre("Catalogue IoT");
        projet.setStatut(StatutProjet.VALIDE);
        projet.setEncadrant(encadrant);
        projet.setSujetId(42L);
    }

    @Test
    void findByProjet_whenCatalogueEmpty_shouldFallbackToSujetLivrables() {
        SujetProjet sujet = new SujetProjet();
        sujet.setId(42L);

        Livrable sujetLivrable = new Livrable();
        sujetLivrable.setId(99L);
        sujetLivrable.setSujetProjet(sujet);
        sujetLivrable.setTypeLivrable(TypeLivrable.RAPPORT);
        sujetLivrable.setNom("Rapport final");
        sujetLivrable.setOriginalFileName("rapport.pdf");
        sujetLivrable.setObjectName("livrables/42/rapport.pdf");
        sujetLivrable.setDeposant(encadrant);
        sujetLivrable.setDateDepot(LocalDateTime.now());
        sujetLivrable.setActif(true);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(projetRepository.findById(55L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(55L)).thenReturn(List.of());
        when(sujetLivrableRepository.findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L))
                .thenReturn(List.of(sujetLivrable));

        List<LivrableCatalogueResponse> result = livrableCatalogueService.findByProjet(55L);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).id());
        assertEquals(55L, result.get(0).projetId());
        assertEquals("Rapport final", result.get(0).nom());
        assertTrue(Boolean.TRUE.equals(result.get(0).fromSujet()));
        verify(sujetLivrableRepository).findBySujetProjetIdAndActifTrueOrderByDateDepotDesc(42L);
    }

    @Test
    void findByProjet_whenCatalogueHasLivrables_shouldNotFallback() {
        LivrableCatalogue catalogueLivrable = new LivrableCatalogue();
        catalogueLivrable.setId(10L);
        catalogueLivrable.setProjet(projet);
        catalogueLivrable.setTypeLivrable(TypeLivrable.DOCUMENTATION);
        catalogueLivrable.setNom("Doc catalogue");
        catalogueLivrable.setDeposant(encadrant);
        catalogueLivrable.setDateDepot(LocalDateTime.now());
        catalogueLivrable.setActif(true);

        when(userService.getCurrentUser()).thenReturn(encadrant);
        when(projetRepository.findById(55L)).thenReturn(Optional.of(projet));
        when(livrableRepository.findByProjetIdAndActifTrueOrderByDateDepotDesc(55L))
                .thenReturn(List.of(catalogueLivrable));

        List<LivrableCatalogueResponse> result = livrableCatalogueService.findByProjet(55L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        assertFalse(Boolean.TRUE.equals(result.get(0).fromSujet()));
    }

    @Test
    void hasActiveLivrables_whenOnlySujetHasFiles_shouldReturnTrue() {
        when(projetRepository.findById(55L)).thenReturn(Optional.of(projet));
        when(livrableRepository.existsByProjetIdAndActifTrue(55L)).thenReturn(false);
        when(sujetLivrableRepository.existsBySujetProjetIdAndActifTrue(42L)).thenReturn(true);

        assertTrue(livrableCatalogueService.hasActiveLivrables(55L));
    }
}

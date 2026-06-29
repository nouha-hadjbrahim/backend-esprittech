package tn.esprit.backend_esprittech.service.sujetprojet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tn.esprit.backend_esprittech.dto.sujetprojet.request.ReferenceItemRequest;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetDomaine;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetPrerequis;
import tn.esprit.backend_esprittech.entity.sujetprojet.SujetTechnologie;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetDomaineRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetPrerequisRepository;
import tn.esprit.backend_esprittech.repository.sujetprojet.SujetTechnologieRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SujetReferenceServiceTest {

    @Mock
    private SujetDomaineRepository domaineRepository;

    @Mock
    private SujetPrerequisRepository prerequisRepository;

    @Mock
    private SujetTechnologieRepository technologieRepository;

    private SujetReferenceService service;

    @BeforeEach
    void setUp() {
        service = new SujetReferenceService(domaineRepository, prerequisRepository, technologieRepository);
    }

    @Test
    void getDomaines_withSearch_shouldUseContainingQueryAndMapPage() {
        SujetDomaine domaine = domaine(1L, "IA");
        when(domaineRepository.findByNomContainingIgnoreCase(eq("ia"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(domaine)));

        var response = service.getDomaines(0, 10, " ia ");

        assertEquals(1, response.content().size());
        assertEquals("IA", response.content().get(0).nom());
        verify(domaineRepository).findByNomContainingIgnoreCase(eq("ia"), any(Pageable.class));
        verify(domaineRepository, never()).findAllByOrderByNomAsc(any(Pageable.class));
    }

    @Test
    void getDomaines_withoutSearch_shouldUseOrderedQuery() {
        when(domaineRepository.findAllByOrderByNomAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(domaine(1L, "Cloud"))));

        var response = service.getDomaines(0, 5, " ");

        assertEquals(1, response.totalElements());
        assertEquals("Cloud", response.content().get(0).nom());
        verify(domaineRepository).findAllByOrderByNomAsc(any(Pageable.class));
        verify(domaineRepository, never()).findByNomContainingIgnoreCase(any(), any(Pageable.class));
    }

    @Test
    void createDomaine_duplicateName_shouldRejectWithoutSaving() {
        when(domaineRepository.existsByNomIgnoreCase("IA")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createDomaine(new ReferenceItemRequest(" IA ")));

        assertEquals("Ce domaine existe déjà", ex.getMessage());
        verify(domaineRepository, never()).save(any());
    }

    @Test
    void updateDomaine_existingDuplicateWithDifferentId_shouldReject() {
        SujetDomaine current = domaine(1L, "Cloud");
        SujetDomaine duplicate = domaine(2L, "IA");
        when(domaineRepository.findById(1L)).thenReturn(Optional.of(current));
        when(domaineRepository.findByNomIgnoreCase("IA")).thenReturn(Optional.of(duplicate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateDomaine(1L, new ReferenceItemRequest("IA")));

        assertEquals("Ce domaine existe déjà", ex.getMessage());
        verify(domaineRepository, never()).save(any());
    }

    @Test
    void findOrCreateDomaine_existingName_shouldReturnExistingWithoutSaving() {
        SujetDomaine existing = domaine(5L, "Cybersecurite");
        when(domaineRepository.findByNomIgnoreCase("Cybersecurite")).thenReturn(Optional.of(existing));

        var response = service.findOrCreateDomaine(" Cybersecurite ");

        assertEquals(5L, response.id());
        verify(domaineRepository, never()).save(any());
    }

    @Test
    void deleteDomaine_missingId_shouldThrowResourceNotFound() {
        when(domaineRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.deleteDomaine(404L));

        assertEquals("Domaine introuvable", ex.getMessage());
        verify(domaineRepository, never()).delete(any());
    }

    @Test
    void countAndAllNames_shouldDelegateToRepositories() {
        when(domaineRepository.count()).thenReturn(2L);
        when(prerequisRepository.count()).thenReturn(3L);
        when(domaineRepository.findAllByOrderByNomAsc()).thenReturn(List.of(domaine(1L, "Cloud")));
        when(technologieRepository.findAllByOrderByNomAsc()).thenReturn(List.of(technologie(2L, "Angular")));

        assertEquals(2L, service.countDomaines());
        assertEquals(3L, service.countPrerequis());
        assertEquals(List.of("Cloud"), service.getAllDomaineNoms());
        assertEquals(List.of("Angular"), service.getAllTechnologieNoms());
    }

    @Test
    void findOrCreateDomaine_newName_shouldSaveNormalizedName() {
        when(domaineRepository.findByNomIgnoreCase("Cybersecurite")).thenReturn(Optional.empty());
        when(domaineRepository.save(any(SujetDomaine.class))).thenAnswer(invocation -> {
            SujetDomaine saved = invocation.getArgument(0);
            saved.setId(6L);
            return saved;
        });

        var response = service.findOrCreateDomaine(" Cybersecurite ");

        assertEquals(6L, response.id());
        assertEquals("Cybersecurite", response.nom());
    }

    @Test
    void prerequisCrud_shouldCreateUpdateListAndDelete() {
        SujetPrerequis prerequis = prerequis(1L, "Java");
        when(prerequisRepository.existsByNomIgnoreCase("Java")).thenReturn(false);
        when(prerequisRepository.save(any(SujetPrerequis.class))).thenAnswer(invocation -> {
            SujetPrerequis saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        assertEquals("Java", service.createPrerequis(new ReferenceItemRequest(" Java ")).nom());

        when(prerequisRepository.findAllByOrderByNomAsc()).thenReturn(List.of(prerequis));
        assertEquals(List.of("Java"), service.getAllPrerequisNoms());

        when(prerequisRepository.findById(1L)).thenReturn(Optional.of(prerequis));
        when(prerequisRepository.findByNomIgnoreCase("Spring")).thenReturn(Optional.empty());
        assertEquals("Spring", service.updatePrerequis(1L, new ReferenceItemRequest("Spring")).nom());

        service.deletePrerequis(1L);
        verify(prerequisRepository).delete(prerequis);
    }

    @Test
    void getPrerequis_withAndWithoutSearch_shouldUseExpectedQueries() {
        when(prerequisRepository.findByNomContainingIgnoreCase(eq("ja"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(prerequis(1L, "Java"))));
        when(prerequisRepository.findAllByOrderByNomAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(prerequis(2L, "Docker"))));

        var searched = service.getPrerequis(0, 10, " ja ");
        var all = service.getPrerequis(0, 10, "");

        assertEquals("Java", searched.content().get(0).nom());
        assertEquals("Docker", all.content().get(0).nom());
    }

    @Test
    void findOrCreatePrerequis_existingAndNewNames_shouldReturnExpectedItems() {
        SujetPrerequis existing = prerequis(1L, "Java");
        when(prerequisRepository.findByNomIgnoreCase("Java")).thenReturn(Optional.of(existing));

        var existingResponse = service.findOrCreatePrerequis(" Java ");

        assertEquals(1L, existingResponse.id());

        when(prerequisRepository.findByNomIgnoreCase("Spring")).thenReturn(Optional.empty());
        when(prerequisRepository.save(any(SujetPrerequis.class))).thenAnswer(invocation -> {
            SujetPrerequis saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        var createdResponse = service.findOrCreatePrerequis("Spring");

        assertEquals(2L, createdResponse.id());
        assertEquals("Spring", createdResponse.nom());
    }

    @Test
    void updatePrerequis_duplicateDifferentId_shouldReject() {
        SujetPrerequis current = prerequis(1L, "Java");
        SujetPrerequis duplicate = prerequis(2L, "Spring");
        when(prerequisRepository.findById(1L)).thenReturn(Optional.of(current));
        when(prerequisRepository.findByNomIgnoreCase("Spring")).thenReturn(Optional.of(duplicate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updatePrerequis(1L, new ReferenceItemRequest("Spring")));

        assertEquals("Ce pr\u00e9requis existe d\u00e9j\u00e0", ex.getMessage());
        verify(prerequisRepository, never()).save(any());
    }

    @Test
    void technologieCrud_shouldCreateFindOrCreateCountAndDelete() {
        SujetTechnologie technologie = technologie(2L, "Angular");
        when(technologieRepository.count()).thenReturn(3L);
        assertEquals(3L, service.countTechnologies());

        when(technologieRepository.existsByNomIgnoreCase("Angular")).thenReturn(false);
        when(technologieRepository.save(any(SujetTechnologie.class))).thenAnswer(invocation -> {
            SujetTechnologie saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertEquals("Angular", service.createTechnologie(new ReferenceItemRequest("Angular")).nom());

        when(technologieRepository.findByNomIgnoreCase("React")).thenReturn(Optional.empty());
        when(technologieRepository.save(any(SujetTechnologie.class))).thenAnswer(invocation -> {
            SujetTechnologie saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        assertEquals("React", service.findOrCreateTechnologie("React").nom());

        when(technologieRepository.findById(2L)).thenReturn(Optional.of(technologie));
        service.deleteTechnologie(2L);
        verify(technologieRepository).delete(technologie);
    }

    @Test
    void getTechnologies_withAndWithoutSearch_shouldUseExpectedQueries() {
        when(technologieRepository.findByNomContainingIgnoreCase(eq("ang"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(technologie(2L, "Angular"))));
        when(technologieRepository.findAllByOrderByNomAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(technologie(3L, "React"))));

        var searched = service.getTechnologies(0, 10, " ang ");
        var all = service.getTechnologies(0, 10, null);

        assertEquals("Angular", searched.content().get(0).nom());
        assertEquals("React", all.content().get(0).nom());
    }

    @Test
    void updateTechnologie_sameExistingId_shouldAllowRenameAndSave() {
        SujetTechnologie technologie = technologie(2L, "Angular");
        when(technologieRepository.findById(2L)).thenReturn(Optional.of(technologie));
        when(technologieRepository.findByNomIgnoreCase("Angular")).thenReturn(Optional.of(technologie));
        when(technologieRepository.save(technologie)).thenReturn(technologie);

        var response = service.updateTechnologie(2L, new ReferenceItemRequest(" Angular "));

        assertEquals("Angular", response.nom());
        verify(technologieRepository).save(technologie);
    }

    @Test
    void updateTechnologie_duplicateDifferentId_shouldReject() {
        SujetTechnologie current = technologie(2L, "Angular");
        SujetTechnologie duplicate = technologie(3L, "React");
        when(technologieRepository.findById(2L)).thenReturn(Optional.of(current));
        when(technologieRepository.findByNomIgnoreCase("React")).thenReturn(Optional.of(duplicate));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateTechnologie(2L, new ReferenceItemRequest("React")));

        assertEquals("Cette technologie existe d\u00e9j\u00e0", ex.getMessage());
        verify(technologieRepository, never()).save(any());
    }

    @Test
    void missingPrerequisAndTechnologie_shouldThrowResourceNotFound() {
        when(prerequisRepository.findById(404L)).thenReturn(Optional.empty());
        when(technologieRepository.findById(405L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deletePrerequis(404L));
        assertThrows(ResourceNotFoundException.class, () -> service.deleteTechnologie(405L));
    }

    @Test
    void createReference_blankName_shouldReject() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createTechnologie(new ReferenceItemRequest(" ")));

        assertEquals("Le nom est obligatoire", ex.getMessage());
        verify(technologieRepository, never()).save(any());
    }

    @Test
    void createTechnology_duplicate_shouldRejectWithoutSaving() {
        when(technologieRepository.existsByNomIgnoreCase("Angular")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createTechnologie(new ReferenceItemRequest("Angular")));

        assertEquals("Cette technologie existe d\u00e9j\u00e0", ex.getMessage());
        verify(technologieRepository, never()).save(any());
    }

    private SujetDomaine domaine(Long id, String nom) {
        return SujetDomaine.builder()
                .id(id)
                .nom(nom)
                .dateCreation(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

    private SujetPrerequis prerequis(Long id, String nom) {
        return SujetPrerequis.builder()
                .id(id)
                .nom(nom)
                .dateCreation(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

    private SujetTechnologie technologie(Long id, String nom) {
        return SujetTechnologie.builder()
                .id(id)
                .nom(nom)
                .dateCreation(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }
}

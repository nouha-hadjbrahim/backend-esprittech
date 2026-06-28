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
    void createReference_blankName_shouldReject() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createTechnologie(new ReferenceItemRequest(" ")));

        assertEquals("Le nom est obligatoire", ex.getMessage());
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

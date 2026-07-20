package tn.esprit.backend_esprittech.service.equipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineRequest;
import tn.esprit.backend_esprittech.dto.equipe.EquipeDomaineResponse;
import tn.esprit.backend_esprittech.dto.response.PagedResponse;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.equipe.EquipeDomaineRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeDomaineServiceTest {

    @Mock
    private EquipeDomaineRepository repository;

    @InjectMocks
    private EquipeDomaineService service;

    private final LocalDateTime now = LocalDateTime.now();
    private final EquipeDomaine domaine = EquipeDomaine.builder().id(1L).nom("Informatique").dateCreation(now).build();
    private final EquipeDomaine domaine2 = EquipeDomaine.builder().id(2L).nom("Réseaux").dateCreation(now).build();

    // ---------------- getPage ----------------

    @Test
    void getPage_withoutSearch_returnsPage() {
        Page<EquipeDomaine> page = new PageImpl<>(List.of(domaine, domaine2));
        when(repository.findAllByOrderByNomAsc(any(Pageable.class))).thenReturn(page);

        PagedResponse<EquipeDomaineResponse> result = service.getPage(0, 10, null);

        assertEquals(2, result.content().size());
        assertEquals("Informatique", result.content().get(0).nom());
        verify(repository).findAllByOrderByNomAsc(any(Pageable.class));
    }

    @Test
    void getPage_withSearch_returnsFilteredPage() {
        Page<EquipeDomaine> page = new PageImpl<>(List.of(domaine));
        when(repository.findByNomContainingIgnoreCase(eq("info"), any(Pageable.class))).thenReturn(page);

        PagedResponse<EquipeDomaineResponse> result = service.getPage(0, 10, "  info  ");

        assertEquals(1, result.content().size());
        verify(repository).findByNomContainingIgnoreCase(eq("info"), any(Pageable.class));
    }

    // ---------------- getAll ----------------

    @Test
    void getAll_returnsAllDomaines() {
        when(repository.findAllByOrderByNomAsc()).thenReturn(List.of(domaine, domaine2));

        List<EquipeDomaineResponse> result = service.getAll();

        assertEquals(2, result.size());
        verify(repository).findAllByOrderByNomAsc();
    }

    // ---------------- count ----------------

    @Test
    void count_returnsTotal() {
        when(repository.count()).thenReturn(5L);

        long result = service.count();

        assertEquals(5L, result);
    }

    // ---------------- create ----------------

    @Test
    void create_validRequest_returnsResponse() {
        when(repository.existsByNomIgnoreCase("IA")).thenReturn(false);
        when(repository.save(any())).thenReturn(domaine);

        EquipeDomaineResponse result = service.create(new EquipeDomaineRequest("IA"));

        assertNotNull(result);
        assertEquals("Informatique", result.nom());
        verify(repository).save(argThat(e -> e.getNom().equals("IA")));
    }

    @Test
    void create_duplicateName_throws() {
        when(repository.existsByNomIgnoreCase("Informatique")).thenReturn(true);
        var req = new EquipeDomaineRequest("Informatique");

        assertThrows(IllegalArgumentException.class,
                () -> service.create(req));
        verify(repository, never()).save(any());
    }

    @Test
    void create_emptyName_throws() {
        var req = new EquipeDomaineRequest("   ");

        assertThrows(IllegalArgumentException.class,
                () -> service.create(req));
        verify(repository, never()).save(any());
    }

    // ---------------- update ----------------

    @Test
    void update_validRequest_returnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));
        when(repository.findByNomIgnoreCase("Data Science")).thenReturn(Optional.empty());
        when(repository.save(domaine)).thenReturn(domaine);

        service.update(1L, new EquipeDomaineRequest("Data Science"));

        assertEquals("Data Science", domaine.getNom());
        verify(repository).save(domaine);
    }

    @Test
    void update_duplicateName_throws() {
        EquipeDomaine existing = EquipeDomaine.builder().id(3L).nom("Data Science").build();
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));
        when(repository.findByNomIgnoreCase("Data Science")).thenReturn(Optional.of(existing));
        var req = new EquipeDomaineRequest("Data Science");

        assertThrows(IllegalArgumentException.class,
                () -> service.update(1L, req));
        verify(repository, never()).save(any());
    }

    @Test
    void update_sameNameDifferentId_allows() {
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));
        when(repository.findByNomIgnoreCase("Informatique")).thenReturn(Optional.of(domaine));
        when(repository.save(domaine)).thenReturn(domaine);

        EquipeDomaineResponse result = service.update(1L, new EquipeDomaineRequest("Informatique"));

        assertNotNull(result);
        verify(repository).save(domaine);
    }

    @Test
    void update_notFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var req = new EquipeDomaineRequest("New");

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, req));
    }

    @Test
    void update_emptyName_throws() {
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));
        var req = new EquipeDomaineRequest("   ");

        assertThrows(IllegalArgumentException.class,
                () -> service.update(1L, req));
        verify(repository, never()).save(any());
    }

    // ---------------- delete ----------------

    @Test
    void delete_existing_deletes() {
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));

        service.delete(1L);

        verify(repository).delete(domaine);
    }

    @Test
    void delete_notFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    // ---------------- getEntityById ----------------

    @Test
    void getEntityById_existing_returnsEntity() {
        when(repository.findById(1L)).thenReturn(Optional.of(domaine));

        EquipeDomaine result = service.getEntityById(1L);

        assertSame(domaine, result);
    }

    @Test
    void getEntityById_notFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEntityById(99L));
    }

    // ---------------- toResponse ----------------

    @Test
    void toResponse_mapsAllFields() {
        EquipeDomaineResponse result = service.toResponse(domaine);

        assertEquals(1L, result.id());
        assertEquals("Informatique", result.nom());
        assertEquals(now, result.dateCreation());
    }
}

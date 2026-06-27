package tn.esprit.backend_esprittech.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.entity.equipe.EquipeDomaine;
import tn.esprit.backend_esprittech.repository.equipe.EquipeDomaineRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeDomaineDataInitializerTest {

    @Mock private EquipeDomaineRepository domaineRepository;

    @Test
    void run_noDomaines_seedsDefaults() {
        when(domaineRepository.count()).thenReturn(0L);

        EquipeDomaineDataInitializer init = new EquipeDomaineDataInitializer(domaineRepository);
        init.run();

        verify(domaineRepository, atLeastOnce()).save(any(EquipeDomaine.class));
    }

    @Test
    void run_domainesAlreadyExist_skips() {
        when(domaineRepository.count()).thenReturn(5L);

        EquipeDomaineDataInitializer init = new EquipeDomaineDataInitializer(domaineRepository);
        init.run();

        verify(domaineRepository, never()).save(any());
    }
}

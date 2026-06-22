package tn.esprit.backend_esprittech.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.entity.ReferentielUtilisateur;
import tn.esprit.backend_esprittech.enums.TypeUtilisateur;
import tn.esprit.backend_esprittech.exception.ReferentielNotFoundException;
import tn.esprit.backend_esprittech.repository.ReferentielUtilisateurRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferentielServiceTest {

    @Mock private ReferentielUtilisateurRepository referentielRepository;
    @InjectMocks private ReferentielService referentielService;

    private MultipartFile csv(String content) {
        return new MockMultipartFile("file", "ref.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    // ---- importerCsv ----

    @Test
    void importerCsv_validRows_savesAllAndReturnsCount() {
        String content = """
                nom,prenom,email,identifiant,type,telephone
                Ben Ali,Mohamed,mohamed@esprit.tn,EA1,ENSEIGNANT,+216 1
                Trabelsi,Sarra,sarra@esprit.tn,ET1,ETUDIANT,+216 2
                """;
        when(referentielRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(referentielRepository.save(any(ReferentielUtilisateur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int imported = referentielService.importerCsv(csv(content));

        assertEquals(2, imported);
        verify(referentielRepository, times(2)).save(any(ReferentielUtilisateur.class));
    }

    @Test
    void importerCsv_skipsHeaderAndInvalidLines() {
        String content = """
                nom,prenom,email,identifiant,type,telephone
                Valide,User,valide@esprit.tn,ID1,ETUDIANT,+216 1
                Sans,Email,,ID2,ETUDIANT,+216 2
                Mauvais,Type,bad@esprit.tn,ID3,INCONNU,+216 3
                Trop,Court,short@esprit.tn
                """;
        when(referentielRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(referentielRepository.save(any(ReferentielUtilisateur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int imported = referentielService.importerCsv(csv(content));

        assertEquals(1, imported);
        verify(referentielRepository, times(1)).save(any(ReferentielUtilisateur.class));
    }

    @Test
    void importerCsv_updatesExistingEntryByEmail() {
        ReferentielUtilisateur existing = ReferentielUtilisateur.builder()
                .id(7L).email("mohamed@esprit.tn").identifiant("OLD").nom("X").prenom("Y")
                .type(TypeUtilisateur.ETUDIANT).build();
        String content = """
                nom,prenom,email,identifiant,type,telephone
                Ben Ali,Mohamed,mohamed@esprit.tn,EA1,ENSEIGNANT,+216 1
                """;
        when(referentielRepository.findByEmail("mohamed@esprit.tn")).thenReturn(Optional.of(existing));
        when(referentielRepository.save(any(ReferentielUtilisateur.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int imported = referentielService.importerCsv(csv(content));

        assertEquals(1, imported);
        // L'entrée existante est mise à jour (même id), pas une nouvelle créée
        assertEquals(TypeUtilisateur.ENSEIGNANT, existing.getType());
        assertEquals("EA1", existing.getIdentifiant());
    }

    @Test
    void importerCsv_emptyFile_throws() {
        MultipartFile empty = new MockMultipartFile("file", "ref.csv", "text/csv", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> referentielService.importerCsv(empty));
        verify(referentielRepository, never()).save(any());
    }

    @Test
    void importerCsv_nullFile_throws() {
        assertThrows(IllegalArgumentException.class, () -> referentielService.importerCsv(null));
    }

    // ---- trouverCorrespondance ----

    @Test
    void trouverCorrespondance_found_returnsEntry() {
        ReferentielUtilisateur ref = ReferentielUtilisateur.builder()
                .id(1L).email("a@esprit.tn").identifiant("ID1")
                .type(TypeUtilisateur.ENSEIGNANT).nom("A").prenom("B").build();
        when(referentielRepository.findByEmailOrIdentifiant("a@esprit.tn", "ID1"))
                .thenReturn(Optional.of(ref));

        ReferentielUtilisateur result = referentielService.trouverCorrespondance("a@esprit.tn", "ID1");

        assertEquals(ref, result);
    }

    @Test
    void trouverCorrespondance_notFound_throws() {
        when(referentielRepository.findByEmailOrIdentifiant("x@esprit.tn", "Z"))
                .thenReturn(Optional.empty());

        assertThrows(ReferentielNotFoundException.class,
                () -> referentielService.trouverCorrespondance("x@esprit.tn", "Z"));
    }
}

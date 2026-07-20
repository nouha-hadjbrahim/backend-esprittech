package tn.esprit.backend_esprittech.service.industrialisation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend_esprittech.dto.industrialisation.QuestionIndustrialisationRequest;
import tn.esprit.backend_esprittech.entity.critereeval.TypeCritere;
import tn.esprit.backend_esprittech.entity.industrialisation.QuestionIndustrialisation;
import tn.esprit.backend_esprittech.entity.industrialisation.TypeReponseIndustrialisation;
import tn.esprit.backend_esprittech.exception.BusinessException;
import tn.esprit.backend_esprittech.exception.ResourceNotFoundException;
import tn.esprit.backend_esprittech.repository.industrialisation.QuestionIndustrialisationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionIndustrialisationServiceTest {

    @Mock
    private QuestionIndustrialisationRepository repository;

    @InjectMocks
    private QuestionIndustrialisationService service;

    private QuestionIndustrialisation question;

    @BeforeEach
    void setUp() {
        question = new QuestionIndustrialisation();
        question.setId(10L);
        question.setLibelle("Depot Git disponible ?");
        question.setDescription("Lien du depot Git");
        question.setTypeReponse(TypeReponseIndustrialisation.BOOLEAN);
        question.setObligatoire(true);
        question.setTypeCritere(TypeCritere.ELIMINATOIRE);
        question.setPoids(null);
        question.setOrdre(1);
        question.setActif(true);
        question.setConditionEliminatoire(true);
        question.setDateCreation(LocalDateTime.of(2026, 1, 1, 8, 0));
        question.setDateMiseAJour(LocalDateTime.of(2026, 1, 2, 8, 0));
    }

    @Test
    void create_validRequest_shouldTrimValuesAndApplyDefaults() {
        when(repository.save(any(QuestionIndustrialisation.class))).thenAnswer(invocation -> {
            QuestionIndustrialisation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        var response = service.create(new QuestionIndustrialisationRequest(
                "  Depot Git disponible ?  ",
                "  Lien du depot  ",
                TypeReponseIndustrialisation.BOOLEAN,
                null,
                TypeCritere.ELIMINATOIRE,
                null,
                1,
                null,
                true
        ));

        assertEquals(10L, response.id());
        assertEquals("Depot Git disponible ?", response.libelle());
        assertEquals("Lien du depot", response.description());
        assertTrue(response.obligatoire());
        assertTrue(response.actif());

        ArgumentCaptor<QuestionIndustrialisation> captor = ArgumentCaptor.forClass(QuestionIndustrialisation.class);
        verify(repository).save(captor.capture());
        assertEquals("Depot Git disponible ?", captor.getValue().getLibelle());
        assertEquals("Lien du depot", captor.getValue().getDescription());
    }

    @Test
    void create_blankLibelle_shouldRejectRequest() {
        QuestionIndustrialisationRequest request = validRequest(" ");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("Le libelle est obligatoire.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_invalidOrder_shouldRejectRequest() {
        QuestionIndustrialisationRequest request = new QuestionIndustrialisationRequest(
                "Question",
                null,
                TypeReponseIndustrialisation.TEXTE,
                true,
                TypeCritere.NOTE,
                1.0,
                0,
                true,
                false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("L'ordre doit etre superieur ou egal a 1.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void create_negativeWeight_shouldRejectRequest() {
        QuestionIndustrialisationRequest request = new QuestionIndustrialisationRequest(
                "Question",
                null,
                TypeReponseIndustrialisation.TEXTE,
                true,
                TypeCritere.NOTE,
                -1.0,
                1,
                true,
                false
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals("Le poids ne peut pas etre negatif.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void update_existingQuestion_shouldPersistChanges() {
        when(repository.findById(10L)).thenReturn(Optional.of(question));
        when(repository.save(any(QuestionIndustrialisation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(10L, new QuestionIndustrialisationRequest(
                "Documentation API",
                " ",
                TypeReponseIndustrialisation.URL,
                false,
                TypeCritere.NOTE,
                2.0,
                3,
                false,
                false
        ));

        assertEquals("Documentation API", response.libelle());
        assertNull(response.description());
        assertEquals(TypeReponseIndustrialisation.URL, response.typeReponse());
        assertFalse(response.obligatoire());
        assertEquals(2.0, response.poids());
        assertFalse(response.actif());
        verify(repository).findById(10L);
        verify(repository).save(question);
    }

    @Test
    void update_missingQuestion_shouldThrowResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, validRequest("Question")));

        assertEquals("Question industrialisation introuvable avec id: 99", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void activate_shouldSetActifTrue() {
        question.setActif(false);
        when(repository.findById(10L)).thenReturn(Optional.of(question));

        service.activate(10L);

        assertTrue(question.getActif());
        verify(repository).save(question);
    }

    @Test
    void deactivate_shouldSetActifFalse() {
        when(repository.findById(10L)).thenReturn(Optional.of(question));

        service.deactivate(10L);

        assertFalse(question.getActif());
        verify(repository).save(question);
    }

    @Test
    void findAll_shouldReturnOrderedMappedResponses() {
        when(repository.findAllByOrderByOrdreAsc()).thenReturn(List.of(question));

        var responses = service.findAll();

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).id());
        assertEquals("Depot Git disponible ?", responses.get(0).libelle());
        verify(repository).findAllByOrderByOrdreAsc();
    }

    @Test
    void findActive_shouldReturnOnlyActiveMappedResponses() {
        when(repository.findByActifTrueOrderByOrdreAsc()).thenReturn(List.of(question));

        var responses = service.findActive();

        assertEquals(1, responses.size());
        assertTrue(responses.get(0).actif());
        verify(repository).findByActifTrueOrderByOrdreAsc();
    }

    private QuestionIndustrialisationRequest validRequest(String libelle) {
        return new QuestionIndustrialisationRequest(
                libelle,
                null,
                TypeReponseIndustrialisation.TEXTE,
                true,
                TypeCritere.NOTE,
                1.0,
                1,
                true,
                false
        );
    }
}

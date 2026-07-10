package tn.esprit.backend_esprittech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.enums.EntityType;
import tn.esprit.backend_esprittech.enums.Role;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService.LogRequest;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistorisationAspectTest {

    @Mock(lenient = true) private HistoriqueService historiqueService;
    @Mock(lenient = true) private ObjectMapper objectMapper;
    @Mock(lenient = true) private BeanFactory beanFactory;
    @Mock(lenient = true) private ProceedingJoinPoint joinPoint;
    @Mock(lenient = true) private MethodSignature methodSignature;
    @Mock(lenient = true) private Historise historise;
    @Mock(lenient = true) private Authentication authentication;

    @Captor private ArgumentCaptor<LogRequest> logCaptor;

    private HistorisationAspect aspect;
    private final User actor = User.builder().id(1L).nom("Test").prenom("User").role(Role.ROLE_ADMIN).build();
    private final Method noArgMethod;

    @SuppressWarnings("unused")
    private static class DummyService {
        public void handle() {}
    }

    HistorisationAspectTest() throws NoSuchMethodException {
        noArgMethod = DummyService.class.getMethod("handle");
    }

    @BeforeEach
    void setUp() {
        aspect = new HistorisationAspect(historiqueService, objectMapper, beanFactory);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(noArgMethod);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser() {
        when(authentication.getPrincipal()).thenReturn(actor);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ---- 1. Normal flow with all expressions ----

    @Test
    void normalFlow_withAllExpression_logsCorrectly() throws Throwable {
        authenticateUser();
        Object expectedResult = Map.of("id", 42);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(historise.oldValuesExpression()).thenReturn("{'old':'val'}");
        when(historise.newValuesExpression()).thenReturn("{'new':'val'}");
        when(historise.metadataExpression()).thenReturn("{'meta':'val'}");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.UPDATE);
        when(historise.entityType()).thenReturn(EntityType.SUJET_PROJET);
        when(historise.entityIdExpression()).thenReturn("42");
        when(historise.summaryExpression()).thenReturn("'Test summary'");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService).log(logCaptor.capture());
        LogRequest req = logCaptor.getValue();
        assertEquals(ActionType.UPDATE, req.action());
        assertEquals(EntityType.SUJET_PROJET, req.entityType());
        assertEquals(42L, req.entityId());
        assertEquals("Test summary", req.summary());
        assertEquals(Map.of("old", "val"), req.oldValues());
        assertEquals(Map.of("new", "val"), req.newValues());
        assertEquals(Map.of("meta", "val"), req.metadata());
        assertEquals(actor, req.actor());
        assertEquals(Role.ROLE_ADMIN, req.actorRole());
    }

    // ---- 2. Without oldValuesExpression ----

    @Test
    void withoutOldValuesExpression_oldValuesNull() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.DELETE);
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'delete'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNull(logCaptor.getValue().oldValues());
        assertNull(logCaptor.getValue().newValues());
    }

    // ---- 3. OldValues evaluation fails ----

    @Test
    void oldValuesEvaluationFails_logsWarningAndContinues() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("{broken");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.DELETE);
        when(historise.entityType()).thenReturn(EntityType.LIVRABLE);
        when(historise.entityIdExpression()).thenReturn("5");
        when(historise.summaryExpression()).thenReturn("'delete'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNull(logCaptor.getValue().oldValues());
    }

    // ---- 4. Actor is null - short-circuit ----

    @Test
    void actorIsNull_returnsEarlyWithoutLogging() throws Throwable {
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'test'");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService, never()).log(any());
    }

    // ---- 5. Auto-serialization for CREATE/UPDATE ----

    @Test
    void autoSerialization_forCreateAction_convertsResult() throws Throwable {
        authenticateUser();
        Map<String, Object> resultMap = Map.of("id", 42, "name", "test");
        when(joinPoint.proceed()).thenReturn(resultMap);

        TypeFactory typeFactory = mock(TypeFactory.class);
        MapType mapType = mock(MapType.class);
        when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructMapType(Map.class, String.class, Object.class)).thenReturn(mapType);
        when(objectMapper.convertValue(resultMap, mapType)).thenReturn(resultMap);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.CREATE);
        when(historise.entityType()).thenReturn(EntityType.SUJET_PROJET);
        when(historise.entityIdExpression()).thenReturn("42");
        when(historise.summaryExpression()).thenReturn("'create'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNotNull(logCaptor.getValue().newValues());
    }

    @Test
    void autoSerialization_forUpdateAction_convertsResult() throws Throwable {
        authenticateUser();
        Map<String, Object> resultMap = Map.of("id", 1);
        when(joinPoint.proceed()).thenReturn(resultMap);

        TypeFactory typeFactory = mock(TypeFactory.class);
        MapType mapType = mock(MapType.class);
        when(objectMapper.getTypeFactory()).thenReturn(typeFactory);
        when(typeFactory.constructMapType(Map.class, String.class, Object.class)).thenReturn(mapType);
        when(objectMapper.convertValue(resultMap, mapType)).thenReturn(resultMap);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.UPDATE);
        when(historise.entityType()).thenReturn(EntityType.LIVRABLE);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'update'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNotNull(logCaptor.getValue().newValues());
    }

    // ---- 6. Auto-serialization skipped when result is null ----

    @Test
    void autoSerialization_skippedWhenResultIsNull() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(null);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.UPDATE);
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'update'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNull(logCaptor.getValue().newValues());
        verify(objectMapper, never()).convertValue(any(), any(MapType.class));
    }

    // ---- 7. Auto-serialization skipped for non-CREATE/UPDATE actions ----

    @Test
    void autoSerialization_skippedForDeleteAction() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.DELETE);
        when(historise.entityType()).thenReturn(EntityType.LIVRABLE);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'delete'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertNull(logCaptor.getValue().newValues());
        verify(objectMapper, never()).convertValue(any(), any(MapType.class));
    }

    // ---- 8. NewValuesExpression takes precedence over auto-serialization ----

    @Test
    void newValuesExpression_whenPresent_skipsAutoSerialization() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(Map.of("id", 42));

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("{'custom':'val'}");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.CREATE);
        when(historise.entityType()).thenReturn(EntityType.SUJET_PROJET);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'create'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertEquals(Map.of("custom", "val"), logCaptor.getValue().newValues());
        verify(objectMapper, never()).convertValue(any(), any(MapType.class));
    }

    // ---- 9. Metadata expression evaluated ----

    @Test
    void metadataExpression_evaluatedAndPassed() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("{'reason':'test'}");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.DELETE);
        when(historise.entityType()).thenReturn(EntityType.CANDIDATURE);
        when(historise.entityIdExpression()).thenReturn("7");
        when(historise.summaryExpression()).thenReturn("'delete'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertEquals(Map.of("reason", "test"), logCaptor.getValue().metadata());
    }

    // ---- 10. Overall historisation fails - returns result without throwing ----

    @Test
    void historisationOverallFails_returnsResultWithoutThrowing() throws Throwable {
        authenticateUser();
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.CREATE);
        when(historise.entityType()).thenReturn(EntityType.SUJET_PROJET);
        when(historise.entityIdExpression()).thenReturn("42");
        when(historise.summaryExpression()).thenReturn("{broken");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService, never()).log(any());
    }

    // ---- 11. Dynamic actionExpression ----

    @Test
    void dynamicActionExpression_resolvedCorrectly() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("'VALIDATE'");
        when(historise.entityType()).thenReturn(EntityType.SUJET_PROJET);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'validate'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertEquals(ActionType.VALIDATE, logCaptor.getValue().action());
    }

    @Test
    void staticAction_usedWhenActionExpressionEmpty() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.REFUSE);
        when(historise.entityType()).thenReturn(EntityType.CANDIDATURE);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'refuse'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertEquals(ActionType.REFUSE, logCaptor.getValue().action());
    }

    // ---- 12. getCurrentUser with non-User principal ----

    @Test
    void actorIsNull_whenPrincipalNotUser() throws Throwable {
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(authentication.getPrincipal()).thenReturn("not a user");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'test'");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService, never()).log(any());
    }

    // ---- 13. getCurrentUser with null auth ----

    @Test
    void actorIsNull_whenAuthIsNull() throws Throwable {
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'test'");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService, never()).log(any());
    }

    // ---- 14. Proceed exception is propagated ----

    @Test
    void proceedException_isPropagated() throws Throwable {
        when(joinPoint.proceed()).thenThrow(new RuntimeException("business error"));

        assertThrows(RuntimeException.class, () -> aspect.aroundHistorise(joinPoint, historise));
        verify(historiqueService, never()).log(any());
    }

    // ---- 15. Actor null with non-empty oldValuesExpression ----

    @Test
    void actorIsNull_withOldValuesExpression_returnsEarly() throws Throwable {
        Object expectedResult = new Object();
        when(joinPoint.proceed()).thenReturn(expectedResult);

        when(historise.oldValuesExpression()).thenReturn("{'key':'val'}");
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'test'");

        Object result = aspect.aroundHistorise(joinPoint, historise);

        assertSame(expectedResult, result);
        verify(historiqueService, never()).log(any());
    }

    // ---- 16. getCurrentUser returns authenticated User explicitly ----

    @Test
    void getCurrentUser_returnsUser_whenAuthenticated() throws Throwable {
        authenticateUser();
        when(joinPoint.proceed()).thenReturn(new Object());

        when(historise.oldValuesExpression()).thenReturn("");
        when(historise.newValuesExpression()).thenReturn("{'custom':'val'}");
        when(historise.metadataExpression()).thenReturn("");
        when(historise.actionExpression()).thenReturn("");
        when(historise.action()).thenReturn(ActionType.UPDATE);
        when(historise.entityType()).thenReturn(EntityType.USER);
        when(historise.entityIdExpression()).thenReturn("1");
        when(historise.summaryExpression()).thenReturn("'test'");

        aspect.aroundHistorise(joinPoint, historise);

        verify(historiqueService).log(logCaptor.capture());
        assertEquals(actor, logCaptor.getValue().actor());
    }
}

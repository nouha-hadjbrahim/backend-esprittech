package tn.esprit.backend_esprittech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tn.esprit.backend_esprittech.annotation.Historise;
import tn.esprit.backend_esprittech.entity.User;
import tn.esprit.backend_esprittech.enums.ActionType;
import tn.esprit.backend_esprittech.service.historique.HistoriqueService;

import java.lang.reflect.Method;
import java.util.Map;

@Aspect
@Component
public class HistorisationAspect {

    private static final Logger log = LoggerFactory.getLogger(HistorisationAspect.class);

    private final HistoriqueService historiqueService;
    private final SpelExpressionParser spelParser;
    private final ObjectMapper objectMapper;
    private final ParameterNameDiscoverer parameterNameDiscoverer;

    public HistorisationAspect(HistoriqueService historiqueService,
                                SpelExpressionParser spelParser,
                                ObjectMapper objectMapper) {
        this.historiqueService = historiqueService;
        this.spelParser = spelParser;
        this.objectMapper = objectMapper;
        this.parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    @Around("@annotation(historise)")
    public Object aroundHistorise(ProceedingJoinPoint joinPoint, Historise historise) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            StandardEvaluationContext context = buildContext(joinPoint, method, result);

            ActionType action = resolveAction(historise, context);
            Long entityId = evaluate(historise.entityIdExpression(), context, Long.class);
            String summary = evaluate(historise.summaryExpression(), context, String.class);

            User actor = getCurrentUser();
            if (actor == null) return result;

            Map<String, Object> newValues = null;
            if (result != null && (action == ActionType.CREATE || action == ActionType.UPDATE)) {
                newValues = objectMapper.convertValue(result, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class));
            }

            historiqueService.log(action, historise.entityType(), entityId, summary,
                    null, newValues, null, actor, actor.getRole().name());
        } catch (Exception e) {
            log.warn("Échec historisation {} {}", historise.action(), historise.entityType(), e);
        }

        return result;
    }

    private StandardEvaluationContext buildContext(ProceedingJoinPoint joinPoint, Method method, Object result) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                result, method, joinPoint.getArgs(), parameterNameDiscoverer);
        context.setVariable("result", result);
        return context;
    }

    private ActionType resolveAction(Historise historise, StandardEvaluationContext context) {
        if (!historise.actionExpression().isEmpty()) {
            String actionName = evaluate(historise.actionExpression(), context, String.class);
            return ActionType.valueOf(actionName);
        }
        return historise.action();
    }

    private <T> T evaluate(String expression, StandardEvaluationContext context, Class<T> type) {
        Expression expr = spelParser.parseExpression(expression);
        return expr.getValue(context, type);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}

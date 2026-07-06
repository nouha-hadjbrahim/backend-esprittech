package tn.esprit.backend_esprittech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
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
    private final BeanFactory beanFactory;

    public HistorisationAspect(HistoriqueService historiqueService,
                                ObjectMapper objectMapper,
                                BeanFactory beanFactory) {
        this.historiqueService = historiqueService;
        this.spelParser = new SpelExpressionParser();
        this.objectMapper = objectMapper;
        this.beanFactory = beanFactory;
        this.parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    }

    @Around("@annotation(historise)")
    public Object aroundHistorise(ProceedingJoinPoint joinPoint, Historise historise) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Map<String, Object> oldValues = null;

        if (!historise.oldValuesExpression().isEmpty()) {
            try {
                StandardEvaluationContext beforeContext = buildContext(joinPoint, method, null);
                oldValues = evaluate(historise.oldValuesExpression(), beforeContext, Map.class);
            } catch (Exception e) {
                log.warn("Échec évaluation oldValues", e);
            }
        }

        Object result = joinPoint.proceed();

        try {
            StandardEvaluationContext context = buildContext(joinPoint, method, result);

            ActionType action = resolveAction(historise, context);
            Long entityId = evaluate(historise.entityIdExpression(), context, Long.class);
            String summary = evaluate(historise.summaryExpression(), context, String.class);

            User actor = getCurrentUser();
            if (actor == null) return result;

            Map<String, Object> newValues = null;
            if (!historise.newValuesExpression().isEmpty()) {
                newValues = evaluate(historise.newValuesExpression(), context, Map.class);
            } else if (result != null && (action == ActionType.CREATE || action == ActionType.UPDATE)) {
                newValues = objectMapper.convertValue(result, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class));
            }

            Map<String, Object> metadata = null;
            if (!historise.metadataExpression().isEmpty()) {
                metadata = evaluate(historise.metadataExpression(), context, Map.class);
            }

            historiqueService.log(action, historise.entityType(), entityId, summary,
                    oldValues, newValues, metadata, actor, actor.getRole());
        } catch (Exception e) {
            log.warn("Échec historisation {} {}", historise.action(), historise.entityType(), e);
        }

        return result;
    }

    private StandardEvaluationContext buildContext(ProceedingJoinPoint joinPoint, Method method, Object result) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                result, method, joinPoint.getArgs(), parameterNameDiscoverer);
        context.setBeanResolver(new BeanFactoryResolver(beanFactory));
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

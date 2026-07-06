package tn.esprit.backend_esprittech.annotation;

import tn.esprit.backend_esprittech.enums.ActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Historise {
    ActionType action() default ActionType.CREATE;
    String actionExpression() default "";
    String entityType();
    String entityIdExpression();
    String summaryExpression();
}

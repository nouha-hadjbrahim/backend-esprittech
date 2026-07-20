package tn.esprit.backend_esprittech.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        storeContext(context);
    }

    /** Renseigne le contexte partage ; isole l'ecriture du champ statique hors de l'instance. */
    private static synchronized void storeContext(ApplicationContext context) {
        applicationContext = context;
    }

    public static <T> T getBean(Class<T> beanType) {
        return applicationContext.getBean(beanType);
    }
}

package com.company.iacchatbot.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Configuration du moteur de templates Thymeleaf dedie a la generation IaC.
 * Templates en mode TEXT (Terraform .tf et YAML OpenShift/KubeVirt)
 * stockes dans src/main/resources/templates/.
 */
@Configuration
public class IaCTemplateConfig {

    @Bean
    public SpringTemplateEngine iacTemplateEngine(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix("");          // le nom du template inclut l'extension (.tf / .yaml)
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setCheckExistence(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}

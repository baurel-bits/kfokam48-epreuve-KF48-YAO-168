package com.kfokam48.app.common.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorise le frontend (Next.js, {@code app.cors.allowed-origins}) à appeler l'API
 * depuis le navigateur : sans cela l'écran formateur de l'issue #8 ne peut pas
 * consommer {@code POST /api/sessions}.
 */
@Configuration
public class ConfigurationCors implements WebMvcConfigurer {

    private final String[] originesAutorisees;

    public ConfigurationCors(@Value("${app.cors.allowed-origins}") String originesAutorisees) {
        this.originesAutorisees = Arrays.stream(originesAutorisees.split(","))
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(originesAutorisees)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}

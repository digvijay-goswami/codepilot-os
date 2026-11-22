package com.codepilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Arrays;

@Configuration
public class AppConfig {

    // Comma-separated list, set on Render as APP_CORS_ALLOWED_ORIGINS
    // e.g. https://your-app.vercel.app,http://localhost:5173
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        // Trim spaces and trailing slashes so a small typo in the env var doesn't break CORS
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(o -> o.replaceAll("/+$", ""))
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(origins)
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}
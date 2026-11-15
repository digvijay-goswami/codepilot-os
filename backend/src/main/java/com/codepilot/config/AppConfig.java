package com.codepilot.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {

    // Comma-separated list of allowed browser origins, e.g. https://codepilot.vercel.app
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public RestTemplate restTemplate() { 
        return new RestTemplate(); 
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}
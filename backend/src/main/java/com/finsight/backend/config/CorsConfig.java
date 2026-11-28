package com.finsight.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Cross-origin (CORS) rules. Only needed when a separate frontend calls this API from
 * another origin (local dev servers or the Railway URL); the bundled static UI is
 * served from the same origin. SecurityConfig activates these rules through
 * cors(Customizer.withDefaults()).
 */
@Configuration
public class CorsConfig {

    /**
     * Allows the listed origins to call any endpoint with credentials. Add any new
     * frontend origin here. Origins must match exactly as the browser sends them
     * (scheme + host, no path and no trailing slash).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://backend-production-11d639ff.up.railway.app/"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
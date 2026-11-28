package com.finsight.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FinSight Spring Boot backend entry point.
 *
 * Architecture overview:
 *   - Static web UI (src/main/resources/static: HTML/JS/CSS) is served by this app and calls the REST API under /api/**.
 *   - controller/  REST endpoints (JWT protected except /api/auth/**).
 *   - service/ + service/impl/  business logic: CSV upload, transactions, analysis, insights, budgets, profile.
 *   - client/  HTTP clients for the separate Python FastAPI "AI service" (classification, forecast, anomalies, subscriptions, recommendations, alerts).
 *   - repository/  Spring Data JPA repositories (MySQL).
 *   - entity/ and dto/  database models and API request/response objects.
 *   - security/ and config/  JWT authentication, CORS, bean wiring and schema repair.
 *
 * Start order for local development: MySQL, then the AI service (uvicorn, port 8000),
 * then this application (port 8080).
 */
@SpringBootApplication
public class BackendApplication {

    /**
     * Boots the Spring application context.
     */
    public static void main(String[] args) {
        SpringApplication.run(
                BackendApplication.class,
                args
        );
    }
}

/**
 * FinSight File Notes: Provides a public lightweight health endpoint for deployment checks.
 */
package com.finsight.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "finsight-backend");
    }
}

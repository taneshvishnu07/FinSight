package com.finsight.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple smoke-test endpoint for checking that JWT authentication works (GET
 * /api/test/protected returns text only with a valid token). Safe to delete for
 * production.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public String protectedEndpoint() {

        return "JWT authentication successful!";
    }
}
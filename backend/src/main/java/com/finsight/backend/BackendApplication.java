/**
 * FinSight File Notes: Starts the Spring Boot backend application.
 */
package com.finsight.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                BackendApplication.class,
                args
        );
    }
}

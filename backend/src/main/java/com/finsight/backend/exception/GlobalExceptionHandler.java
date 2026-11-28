package com.finsight.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into a consistent JSON error body {status, error, message} for the
 * whole API; the frontend shows `message` to the user. Handlers are matched most-
 * specific first.
 *
 * Mapping: validation failures and IllegalArgumentException -> 400; database
 * constraint violations -> 409; "Financial profile not found." -> 404; any other
 * RuntimeException -> 400 (its message is passed on, with SQL details hidden);
 * everything else -> 500 with a generic message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bean Validation (@Valid) failure: return the first field error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Please check the submitted information.");

        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Bad input from business-rule checks (e.g. invalid CSV data).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, safeMessage(exception, "Invalid request."));
    }

    /**
     * Unique/foreign-key violations: generic conflict message (database details are
     * not exposed).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException exception) {
        return build(HttpStatus.CONFLICT, "This operation conflicts with existing FinSight data. Please refresh and try again.");
    }

    /**
     * Services throw plain RuntimeExceptions for "not found" style problems; they
     * surface as 400, except a missing profile which is 404 so the UI can redirect to
     * profile setup.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(
            RuntimeException exception) {
        String message = userSafeRuntimeMessage(exception);
        if (message.equalsIgnoreCase("Financial profile not found.")) {
            return build(HttpStatus.NOT_FOUND, message);
        }
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Last-resort handler; hides internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred. Please try again.");
    }

    /**
     * Create the standard error response body.
     */
    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String message) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Exception message, or `fallback` if empty.
     */
    private String safeMessage(Exception exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    /**
     * Hide raw SQL / constraint error text from users; other messages pass through
     * unchanged.
     */
    private String userSafeRuntimeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "The request could not be completed.";
        }
        String lower = message.toLowerCase();
        if (lower.contains("duplicate entry")
                || lower.contains("could not execute statement")
                || lower.contains("constraint [")
                || lower.contains("sql [")) {
            return "The operation could not be completed because it conflicts with existing data.";
        }
        return message;
    }
}

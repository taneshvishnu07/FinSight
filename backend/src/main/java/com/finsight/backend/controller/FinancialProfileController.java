/**
 * FinSight File Notes: Exposes REST API endpoints for financial profile operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.financialprofile.FinancialProfileCreateRequest;
import com.finsight.backend.dto.financialprofile.FinancialProfileResponse;
import com.finsight.backend.service.FinancialProfileService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/financial-profile")
@RequiredArgsConstructor
public class FinancialProfileController {

    private final FinancialProfileService financialProfileService;


    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<FinancialProfileResponse> createProfile(
            @Valid @RequestBody FinancialProfileCreateRequest request) {

        FinancialProfileResponse response =
                financialProfileService.createProfile(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // GET MY PROFILE
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<FinancialProfileResponse> getMyProfile() {

        return ResponseEntity.ok(
                financialProfileService.getMyProfile()
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping
    public ResponseEntity<FinancialProfileResponse> updateProfile(
            @Valid @RequestBody FinancialProfileCreateRequest request) {

        return ResponseEntity.ok(
                financialProfileService.updateProfile(request)
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile() {

        financialProfileService.deleteProfile();

        return ResponseEntity.noContent().build();
    }
}
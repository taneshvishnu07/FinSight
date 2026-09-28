/**
 * FinSight File Notes: Exposes REST API endpoints for monthly budget operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.budget.MonthlyBudgetRequest;
import com.finsight.backend.dto.budget.MonthlyBudgetResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.MonthlyBudget;
import com.finsight.backend.entity.User;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.MonthlyBudgetRepository;
import com.finsight.backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/monthly-budgets")
@RequiredArgsConstructor
@Transactional
public class MonthlyBudgetController {

    private final MonthlyBudgetRepository repo;
    private final FinancialProfileRepository profiles;
    private final UserRepository users;

    /**
     * Resolves the currently logged-in user's profile.
     * The GET endpoint handles a missing profile separately so the
     * first-time profile page does not display a misleading server error.
     */
    private FinancialProfile findProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        User user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        return profiles.findByUser(user).orElse(null);
    }

    private FinancialProfile requireProfile() {
        FinancialProfile profile = findProfile();
        if (profile == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Please save your financial profile first."
            );
        }
        return profile;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<MonthlyBudgetResponse> all() {
        FinancialProfile profile = findProfile();
        if (profile == null) {
            return List.of();
        }

        return repo.findAllByFinancialProfileOrderByBudgetMonthAsc(profile)
                .stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();
    }

    @PutMapping
    public MonthlyBudgetResponse save(@Valid @RequestBody MonthlyBudgetRequest request) {
        MonthlyBudget budget = saveOne(requireProfile(), request);
        return toResponse(budget);
    }

    @PutMapping("/bulk")
    public List<MonthlyBudgetResponse> saveBulk(
            @RequestBody @Valid List<@Valid MonthlyBudgetRequest> requests) {

        FinancialProfile financialProfile = requireProfile();
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one monthly budget is required."
            );
        }

        return requests.stream()
                .map(request -> saveOne(financialProfile, request))
                .map(this::toResponse)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        FinancialProfile financialProfile = requireProfile();
        Long budgetId = Objects.requireNonNull(id, "Monthly budget ID must not be null.");
        MonthlyBudget budget = repo.findById(budgetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Monthly budget not found."
                ));

        if (!Objects.equals(
                budget.getFinancialProfile().getId(),
                financialProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorised to modify this monthly budget."
            );
        }

        repo.delete(budget);
        return ResponseEntity.noContent().build();
    }

    private MonthlyBudget saveOne(
            FinancialProfile financialProfile,
            MonthlyBudgetRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Monthly budget data is required."
            );
        }

        BigDecimal amount = request.getAmount();
        if (amount == null
                || amount.signum() < 0
                || amount.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Monthly budget must be a whole number equal to or greater than zero."
            );
        }

        MonthlyBudget budget = repo.findByFinancialProfileAndBudgetMonth(
                        financialProfile,
                        request.getBudgetMonth()
                )
                .orElseGet(MonthlyBudget::new);

        budget.setFinancialProfile(financialProfile);
        budget.setBudgetMonth(request.getBudgetMonth());
        budget.setAmount(amount.setScale(0));

        return repo.save(budget);
    }

    private MonthlyBudgetResponse toResponse(MonthlyBudget budget) {
        return new MonthlyBudgetResponse(
                budget.getId(),
                budget.getBudgetMonth(),
                budget.getAmount()
        );
    }
}

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

/**
 * Per-month budgets (e.g. 2026-03 -> RM 2000) for the logged-in user, mounted under
 * /api/monthly-budgets. Unlike most controllers it talks to repositories directly. The
 * class is @Transactional, so every request runs in one database transaction.
 */
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

    /**
     * Like findProfile() but answers 409 Conflict when the user has not saved a
     * profile yet (budgets are stored on the profile).
     */
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

    /**
     * GET /api/monthly-budgets - all budgets, oldest month first. Returns an empty
     * list (not an error) when there is no profile yet.
     */
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

    /**
     * PUT /api/monthly-budgets - create or update the budget for one month (upsert by
     * month).
     */
    @PutMapping
    public MonthlyBudgetResponse save(@Valid @RequestBody MonthlyBudgetRequest request) {
        MonthlyBudget budget = saveOne(requireProfile(), request);
        return toResponse(budget);
    }

    /**
     * PUT /api/monthly-budgets/bulk - upsert several months at once; at least one
     * entry is required.
     */
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

    /**
     * DELETE /api/monthly-budgets/{id} - delete a budget. 404 if it does not exist,
     * 403 if it belongs to another user's profile.
     */
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

    /**
     * Validate and upsert one month: the amount must be a whole number >= 0 (stored
     * with scale 0). An existing row for the same profile and month is updated,
     * otherwise a new one is created.
     */
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

    /**
     * Entity to API response.
     */
    private MonthlyBudgetResponse toResponse(MonthlyBudget budget) {
        return new MonthlyBudgetResponse(
                budget.getId(),
                budget.getBudgetMonth(),
                budget.getAmount()
        );
    }
}

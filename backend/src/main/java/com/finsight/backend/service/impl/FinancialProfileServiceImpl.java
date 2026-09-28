/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import com.finsight.backend.dto.budget.MonthlyBudgetRequest;
import com.finsight.backend.dto.budget.MonthlyBudgetResponse;
import com.finsight.backend.dto.financialprofile.FinancialProfileCreateRequest;
import com.finsight.backend.dto.financialprofile.FinancialProfileResponse;
import com.finsight.backend.entity.EmployeeDetails;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.MonthlyBudget;
import com.finsight.backend.entity.StudentDetails;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.OccupationType;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.MonthlyBudgetRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.FinancialProfileService;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialProfileServiceImpl implements FinancialProfileService {

    private final FinancialProfileRepository financialProfileRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;
    private final UserRepository userRepository;

    @Override
    public FinancialProfileResponse createProfile(FinancialProfileCreateRequest request) {
        User user = getAuthenticatedUser();
        validateProfileDetails(request);

        if (financialProfileRepository.findByUser(user).isPresent()) {
            throw new IllegalStateException("Financial profile already exists.");
        }

        FinancialProfile profile = new FinancialProfile();
        profile.setUser(user);
        profile.setOccupationType(request.getOccupationType());
        profile.setFinancialGoalMonths(request.getFinancialGoalMonths());

        applyOccupationDetails(profile, request);

        FinancialProfile savedProfile = financialProfileRepository.saveAndFlush(profile);
        saveMonthlyBudgets(savedProfile, request.getMonthlyBudgets());

        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialProfileResponse getMyProfile() {
        User user = getAuthenticatedUser();
        FinancialProfile profile = financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile not found."));
        return mapToResponse(profile);
    }

    @Override
    public FinancialProfileResponse updateProfile(FinancialProfileCreateRequest request) {
        User user = getAuthenticatedUser();
        validateProfileDetails(request);

        FinancialProfile profile = financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile not found."));

        OccupationType requestedType = request.getOccupationType();
        if (profile.getOccupationType() != requestedType) {
            profile.setStudentDetails(null);
            profile.setEmployeeDetails(null);
            profile.setOccupationType(requestedType);
        }

        profile.setFinancialGoalMonths(request.getFinancialGoalMonths());
        applyOccupationDetails(profile, request);

        FinancialProfile updatedProfile = financialProfileRepository.saveAndFlush(profile);
        saveMonthlyBudgets(updatedProfile, request.getMonthlyBudgets());

        return mapToResponse(updatedProfile);
    }

    private void applyOccupationDetails(FinancialProfile profile, FinancialProfileCreateRequest request) {
        if (request.getOccupationType() == OccupationType.STUDENT) {
            StudentDetails details = profile.getStudentDetails();
            if (details == null) {
                details = createStudentDetails(request, profile);
                profile.setStudentDetails(details);
            } else {
                updateStudentDetails(details, request);
            }
            return;
        }

        EmployeeDetails details = profile.getEmployeeDetails();
        if (request.getOccupationType() == OccupationType.EMPLOYEE) {
            if (details == null) {
                details = createEmployeeDetails(request, profile);
                profile.setEmployeeDetails(details);
            } else {
                updateEmployeeDetails(details, request);
            }
        } else if (request.getOccupationType() == OccupationType.OTHER) {
            if (details == null) {
                details = createOtherDetails(request, profile);
                profile.setEmployeeDetails(details);
            } else {
                updateOtherDetails(details, request);
            }
        }
    }

    private void validateProfileDetails(FinancialProfileCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Financial profile data is required.");
        }
        if (request.getFinancialGoalMonths() == null
                || request.getFinancialGoalMonths() < 1
                || request.getFinancialGoalMonths() > 120) {
            throw new IllegalArgumentException("Financial goal period must be between 1 and 120 months.");
        }
        if (request.getOccupationType() == null) {
            throw new IllegalArgumentException("Please select an occupation type.");
        }

        validateMonthlyBudgets(request.getMonthlyBudgets());

        if (request.getOccupationType() == OccupationType.STUDENT) {
            requireText(request.getInstitution(), "Institution is required for student profiles.");
            requireText(request.getCourse(), "Course is required for student profiles.");
            if (request.getStudyYear() == null || request.getStudyYear() < 1 || request.getStudyYear() > 20) {
                throw new IllegalArgumentException("Study year must be between 1 and 20.");
            }
            if (request.getMonthlyAllowance() == null || request.getMonthlyAllowance().signum() < 0) {
                throw new IllegalArgumentException("Monthly allowance must be zero or greater.");
            }
            if (request.getReceivesScholarship() == null) {
                throw new IllegalArgumentException("Please specify whether you receive a scholarship.");
            }
        } else if (request.getOccupationType() == OccupationType.EMPLOYEE) {
            requireText(request.getOccupation(), "Occupation is required for employee profiles.");
            requireText(request.getCompanyName(), "Company name is required for employee profiles.");
            if (request.getMonthlySalary() == null || request.getMonthlySalary().signum() < 0) {
                throw new IllegalArgumentException("Monthly salary must be zero or greater.");
            }
            if (request.getHasSideIncome() == null) {
                throw new IllegalArgumentException("Please specify whether you have side income.");
            }
        } else {
            requireText(request.getOccupation(), "Please enter your occupation.");
        }
    }

    private void validateMonthlyBudgets(List<MonthlyBudgetRequest> requests) {
        if (requests == null) return;
        for (MonthlyBudgetRequest request : requests) {
            if (request == null) {
                throw new IllegalArgumentException("Monthly budget data is invalid.");
            }
            if (request.getBudgetMonth() == null || !request.getBudgetMonth().matches("\\d{4}-(0[1-9]|1[0-2])")) {
                throw new IllegalArgumentException("Each budget month must use YYYY-MM format.");
            }
            BigDecimal amount = request.getAmount();
            if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Monthly budgets must be whole numbers equal to or greater than zero.");
            }
        }
    }

    private void saveMonthlyBudgets(FinancialProfile profile, List<MonthlyBudgetRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        for (MonthlyBudgetRequest request : requests) {
            MonthlyBudget budget = monthlyBudgetRepository
                    .findByFinancialProfileAndBudgetMonth(profile, request.getBudgetMonth())
                    .orElseGet(MonthlyBudget::new);
            budget.setFinancialProfile(profile);
            budget.setBudgetMonth(request.getBudgetMonth());
            budget.setAmount(request.getAmount().setScale(0));
            monthlyBudgetRepository.save(budget);
        }
        monthlyBudgetRepository.flush();
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void deleteProfile() {
        User user = getAuthenticatedUser();
        FinancialProfile profile = financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile not found."));
        financialProfileRepository.delete(Objects.requireNonNull(profile));
    }

    private StudentDetails createStudentDetails(FinancialProfileCreateRequest request, FinancialProfile profile) {
        StudentDetails details = new StudentDetails();
        details.setFinancialProfile(profile);
        details.setInstitution(request.getInstitution());
        details.setCourse(request.getCourse());
        details.setStudyYear(request.getStudyYear());
        details.setMonthlyAllowance(request.getMonthlyAllowance());
        details.setReceivesScholarship(request.getReceivesScholarship());
        return details;
    }

    private EmployeeDetails createEmployeeDetails(FinancialProfileCreateRequest request, FinancialProfile profile) {
        EmployeeDetails details = new EmployeeDetails();
        details.setFinancialProfile(profile);
        details.setOccupation(request.getOccupation());
        details.setCompanyName(request.getCompanyName());
        details.setMonthlySalary(request.getMonthlySalary());
        details.setHasSideIncome(request.getHasSideIncome());
        return details;
    }

    private EmployeeDetails createOtherDetails(FinancialProfileCreateRequest request, FinancialProfile profile) {
        EmployeeDetails details = new EmployeeDetails();
        details.setFinancialProfile(profile);
        details.setOccupation(request.getOccupation().trim());
        details.setCompanyName("Not specified");
        details.setMonthlySalary(BigDecimal.ZERO);
        details.setHasSideIncome(Boolean.FALSE);
        return details;
    }

    private void updateOtherDetails(EmployeeDetails details, FinancialProfileCreateRequest request) {
        details.setOccupation(request.getOccupation().trim());
        details.setCompanyName("Not specified");
        details.setMonthlySalary(BigDecimal.ZERO);
        details.setHasSideIncome(Boolean.FALSE);
    }

    private void updateStudentDetails(StudentDetails details, FinancialProfileCreateRequest request) {
        details.setInstitution(request.getInstitution());
        details.setCourse(request.getCourse());
        details.setStudyYear(request.getStudyYear());
        details.setMonthlyAllowance(request.getMonthlyAllowance());
        details.setReceivesScholarship(request.getReceivesScholarship());
    }

    private void updateEmployeeDetails(EmployeeDetails details, FinancialProfileCreateRequest request) {
        details.setOccupation(request.getOccupation());
        details.setCompanyName(request.getCompanyName());
        details.setMonthlySalary(request.getMonthlySalary());
        details.setHasSideIncome(request.getHasSideIncome());
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("User is not authenticated.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    private FinancialProfileResponse mapToResponse(FinancialProfile profile) {
        StudentDetails student = profile.getStudentDetails();
        EmployeeDetails employee = profile.getEmployeeDetails();

        List<MonthlyBudgetResponse> budgets = new ArrayList<>();
        List<MonthlyBudget> savedBudgets = profile.getMonthlyBudgets();
        if (savedBudgets != null) {
            for (MonthlyBudget budget : savedBudgets) {
                if (budget == null) continue;
                budgets.add(new MonthlyBudgetResponse(budget.getId(), budget.getBudgetMonth(), budget.getAmount()));
            }
        }

        return new FinancialProfileResponse(
                profile.getId(),
                profile.getOccupationType(),
                profile.getFinancialGoalMonths(),
                student == null ? null : student.getInstitution(),
                student == null ? null : student.getCourse(),
                student == null ? null : student.getStudyYear(),
                student == null ? null : student.getMonthlyAllowance(),
                student == null ? null : student.getReceivesScholarship(),
                employee == null ? null : employee.getOccupation(),
                employee == null ? null : employee.getCompanyName(),
                employee == null ? null : employee.getMonthlySalary(),
                employee == null ? null : employee.getHasSideIncome(),
                budgets
        );
    }
}

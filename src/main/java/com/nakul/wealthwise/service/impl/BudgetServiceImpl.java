package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.request.BudgetRequest;
import com.nakul.wealthwise.dto.response.CategoryResponse;
import com.nakul.wealthwise.dto.response.BudgetResponse;
import com.nakul.wealthwise.entity.Budget;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.BudgetRepository;
import com.nakul.wealthwise.repository.CategoryRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public BudgetResponse createBudget(String email, BudgetRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Category category = categoryRepository.findVisibleByIdAndUser(request.getCategoryId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible with ID: " + request.getCategoryId()));

        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set for EXPENSE categories");
        }

        if (budgetRepository.existsByUserEmailAndCategoryIdAndMonthAndYear(email, request.getCategoryId(), request.getMonth(), request.getYear())) {
            throw new IllegalArgumentException("A budget already exists for this category in the specified month and year");
        }

        Budget budget = Budget.builder()
                .category(category)
                .user(user)
                .amount(request.getAmount())
                .month(request.getMonth())
                .year(request.getYear())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Budget savedBudget = budgetRepository.save(budget);
        return mapToResponse(savedBudget, email);
    }

    @Override
    public BudgetResponse updateBudget(Long id, String email, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found or not accessible with ID: " + id));

        Category category = categoryRepository.findVisibleByIdAndUser(request.getCategoryId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible with ID: " + request.getCategoryId()));

        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException("Budgets can only be set for EXPENSE categories");
        }

        // Check if unique constraint is violated (if category, month, or year changed)
        if ((!budget.getCategory().getId().equals(request.getCategoryId())
                || !budget.getMonth().equals(request.getMonth())
                || !budget.getYear().equals(request.getYear()))
                && budgetRepository.existsByUserEmailAndCategoryIdAndMonthAndYear(email, request.getCategoryId(), request.getMonth(), request.getYear())) {
            throw new IllegalArgumentException("A budget already exists for this category in the specified month and year");
        }

        budget.setAmount(request.getAmount());
        budget.setCategory(category);
        budget.setMonth(request.getMonth());
        budget.setYear(request.getYear());
        budget.setUpdatedAt(LocalDateTime.now());

        Budget updatedBudget = budgetRepository.save(budget);
        return mapToResponse(updatedBudget, email);
    }

    @Override
    public void deleteBudget(Long id, String email) {
        Budget budget = budgetRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found or not accessible with ID: " + id));

        budgetRepository.delete(budget);
    }

    @Override
    public BudgetResponse getBudgetById(Long id, String email) {
        Budget budget = budgetRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found or not accessible with ID: " + id));
        return mapToResponse(budget, email);
    }

    @Override
    public List<BudgetResponse> getBudgetsByMonthAndYear(String email, Integer month, Integer year) {
        return budgetRepository.findByUserEmailAndMonthAndYear(email, month, year)
                .stream()
                .map(budget -> mapToResponse(budget, email))
                .collect(Collectors.toList());
    }

    private BudgetResponse mapToResponse(Budget budget, String email) {
        LocalDate startDate = LocalDate.of(budget.getYear(), budget.getMonth(), 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal spent = transactionRepository.getSpentAmountByCategoryAndDateRange(
                email,
                budget.getCategory().getId(),
                startDate,
                endDate
        );

        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        BigDecimal remaining = budget.getAmount().subtract(spent);

        BigDecimal threshold = budget.getAmount().multiply(new BigDecimal("0.90"));
        boolean isAlertActive = spent.compareTo(threshold) > 0;

        CategoryResponse categoryResponse = CategoryResponse.builder()
                .id(budget.getCategory().getId())
                .name(budget.getCategory().getName())
                .type(budget.getCategory().getType())
                .isDefault(budget.getCategory().getIsDefault())
                .build();

        return BudgetResponse.builder()
                .id(budget.getId())
                .category(categoryResponse)
                .amount(budget.getAmount())
                .month(budget.getMonth())
                .year(budget.getYear())
                .spent(spent)
                .remaining(remaining)
                .isAlertActive(isAlertActive)
                .build();
    }
}

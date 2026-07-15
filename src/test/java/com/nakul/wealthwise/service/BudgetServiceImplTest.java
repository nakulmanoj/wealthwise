package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.BudgetRequest;
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
import com.nakul.wealthwise.service.impl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private User user;
    private Category expenseCategory;
    private Category incomeCategory;
    private Budget budget;
    private String email = "test@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email(email)
                .build();

        expenseCategory = Category.builder()
                .id(10L)
                .name("Food & Dining")
                .type(TransactionType.EXPENSE)
                .isDefault(true)
                .build();

        incomeCategory = Category.builder()
                .id(11L)
                .name("Salary")
                .type(TransactionType.INCOME)
                .isDefault(true)
                .build();

        budget = Budget.builder()
                .id(50L)
                .category(expenseCategory)
                .user(user)
                .amount(new BigDecimal("1000.00"))
                .month(7)
                .year(2026)
                .build();
    }

    @Test
    void createBudget_Success() {
        BudgetRequest request = BudgetRequest.builder()
                .categoryId(10L)
                .amount(new BigDecimal("1000.00"))
                .month(7)
                .year(2026)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserEmailAndCategoryIdAndMonthAndYear(email, 10L, 7, 2026)).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq(email), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("400.00"));

        BudgetResponse response = budgetService.createBudget(email, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.getAmount());
        assertEquals(new BigDecimal("400.00"), response.getSpent());
        assertEquals(new BigDecimal("600.00"), response.getRemaining());
        assertFalse(response.getIsAlertActive());
        verify(budgetRepository, times(1)).save(any(Budget.class));
    }

    @Test
    void createBudget_ThrowsIllegalArgumentExceptionForIncomeCategory() {
        BudgetRequest request = BudgetRequest.builder()
                .categoryId(11L) // INCOME ("Salary")
                .amount(new BigDecimal("1000.00"))
                .month(7)
                .year(2026)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(11L, email)).thenReturn(Optional.of(incomeCategory));

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(email, request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_ThrowsIllegalArgumentExceptionForDuplicate() {
        BudgetRequest request = BudgetRequest.builder()
                .categoryId(10L)
                .amount(new BigDecimal("1000.00"))
                .month(7)
                .year(2026)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserEmailAndCategoryIdAndMonthAndYear(email, 10L, 7, 2026)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> budgetService.createBudget(email, request));
        verify(budgetRepository, never()).save(any(Budget.class));
    }

    @Test
    void createBudget_AlertActiveWhenSpentOver90Percent() {
        BudgetRequest request = BudgetRequest.builder()
                .categoryId(10L)
                .amount(new BigDecimal("1000.00"))
                .month(7)
                .year(2026)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(expenseCategory));
        when(budgetRepository.existsByUserEmailAndCategoryIdAndMonthAndYear(email, 10L, 7, 2026)).thenReturn(false);
        when(budgetRepository.save(any(Budget.class))).thenReturn(budget);
        // Spent 950.00 which is > 90% of 1000.00
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq(email), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("950.00"));

        BudgetResponse response = budgetService.createBudget(email, request);

        assertNotNull(response);
        assertTrue(response.getIsAlertActive());
        assertEquals(new BigDecimal("50.00"), response.getRemaining());
    }

    @Test
    void getBudgetById_Success() {
        when(budgetRepository.findByIdAndUserEmail(50L, email)).thenReturn(Optional.of(budget));
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq(email), eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        BudgetResponse response = budgetService.getBudgetById(50L, email);

        assertNotNull(response);
        assertEquals(50L, response.getId());
    }

    @Test
    void getBudgetById_ThrowsResourceNotFoundException() {
        when(budgetRepository.findByIdAndUserEmail(999L, email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> budgetService.getBudgetById(999L, email));
    }

    @Test
    void deleteBudget_Success() {
        when(budgetRepository.findByIdAndUserEmail(50L, email)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(50L, email);

        verify(budgetRepository, times(1)).delete(budget);
    }

    @Test
    void getBudgetsByMonthAndYear_Success() {
        when(budgetRepository.findByUserEmailAndMonthAndYear(email, 7, 2026)).thenReturn(List.of(budget));
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq(email), eq(10L), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        List<BudgetResponse> results = budgetService.getBudgetsByMonthAndYear(email, 7, 2026);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(50L, results.get(0).getId());
    }
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.entity.Budget;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.repository.BudgetRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ScheduledTasksService scheduledTasksService;

    @Test
    void evictMarketPricesCache_RunsSuccessfully() {
        assertDoesNotThrow(() -> scheduledTasksService.evictMarketPricesCache());
    }

    @Test
    void checkBudgetsDaily_Success() {
        User user = User.builder().email("test@example.com").build();
        Category category = Category.builder().id(10L).name("Food").build();
        
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        Budget budget = Budget.builder()
                .id(1L)
                .user(user)
                .category(category)
                .amount(new BigDecimal("100.00"))
                .month(month)
                .year(year)
                .build();

        when(budgetRepository.findAll()).thenReturn(List.of(budget));
        // Spent 95.00 which is > 90%
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq("test@example.com"), eq(10L), any(), any()))
                .thenReturn(new BigDecimal("95.00"));

        assertDoesNotThrow(() -> scheduledTasksService.checkBudgetsDaily());

        verify(budgetRepository, times(1)).findAll();
        verify(transactionRepository, times(1)).getSpentAmountByCategoryAndDateRange(eq("test@example.com"), eq(10L), any(), any());
    }
}

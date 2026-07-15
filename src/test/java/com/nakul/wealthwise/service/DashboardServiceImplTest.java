package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.response.DashboardResponse;
import com.nakul.wealthwise.dto.response.InvestmentResponse;
import com.nakul.wealthwise.entity.Budget;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.Transaction;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.repository.BudgetRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private InvestmentService investmentService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private String email = "test@example.com";

    @Test
    void getDashboardData_Success() {
        Category category = Category.builder().id(5L).name("Food").build();

        Transaction incomeTx = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.now())
                .type(TransactionType.INCOME)
                .category(category)
                .build();

        Transaction expenseTx = Transaction.builder()
                .id(2L)
                .amount(new BigDecimal("200.00"))
                .date(LocalDate.now())
                .type(TransactionType.EXPENSE)
                .category(category)
                .build();

        Budget budget = Budget.builder()
                .id(10L)
                .category(category)
                .amount(new BigDecimal("1000.00"))
                .build();

        InvestmentResponse investment = InvestmentResponse.builder()
                .symbol("AAPL")
                .quantity(new BigDecimal("10"))
                .currentValue(new BigDecimal("1500.00"))
                .build();

        // Dashboard service now calls findAll(Specification) — mock that
        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(incomeTx, expenseTx));
        when(investmentService.getPortfolio(email)).thenReturn(List.of(investment));
        
        LocalDate today = LocalDate.now();
        when(budgetRepository.findByUserEmailAndMonthAndYear(email, today.getMonthValue(), today.getYear()))
                .thenReturn(List.of(budget));
        when(transactionRepository.getSpentAmountByCategoryAndDateRange(eq(email), eq(5L), any(), any()))
                .thenReturn(new BigDecimal("200.00"));

        DashboardResponse response = dashboardService.getDashboardData(email);

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.00"), response.getTotalIncome());
        assertEquals(new BigDecimal("200.00"), response.getTotalExpense());
        assertEquals(new BigDecimal("4800.00"), response.getNetSavings());
        assertEquals(new BigDecimal("1500.00"), response.getPortfolioValue());
        
        // Net worth: Cash balance (allTimeIncome - allTimeExpense) = 4800 + Portfolio Value (1500) = 6300
        assertEquals(new BigDecimal("6300.00"), response.getNetWorth());
        assertEquals(new BigDecimal("1000.00"), response.getBudgetLimit());
        assertEquals(new BigDecimal("200.00"), response.getBudgetSpent());
        assertEquals(new BigDecimal("800.00"), response.getBudgetRemaining());
        assertEquals(1, response.getCategoryDistribution().size());
        assertEquals(6, response.getMonthlyCashFlows().size());
    }
}

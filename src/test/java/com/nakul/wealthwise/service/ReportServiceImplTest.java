package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.response.MonthlyReportResponse;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.Transaction;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private String email = "test@example.com";
    private Transaction incomeTx;
    private Transaction expenseTx;

    @BeforeEach
    void setUp() {
        Category salaryCategory = Category.builder().name("Salary").build();
        Category foodCategory = Category.builder().name("Food").build();

        incomeTx = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("3000.00"))
                .date(LocalDate.of(2026, 7, 5))
                .type(TransactionType.INCOME)
                .category(salaryCategory)
                .description("Monthly Pay")
                .build();

        expenseTx = Transaction.builder()
                .id(2L)
                .amount(new BigDecimal("150.00"))
                .date(LocalDate.of(2026, 7, 10))
                .type(TransactionType.EXPENSE)
                .category(foodCategory)
                .description("Groceries")
                .build();
    }

    @Test
    void getMonthlyReport_Success() {
        // Report service now calls findAll(Specification) — mock that
        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(incomeTx, expenseTx));

        MonthlyReportResponse report = reportService.getMonthlyReport(email, 7, 2026);

        assertNotNull(report);
        assertEquals(7, report.getMonth());
        assertEquals(2026, report.getYear());
        assertEquals(new BigDecimal("3000.00"), report.getTotalIncome());
        assertEquals(new BigDecimal("150.00"), report.getTotalExpense());
        assertEquals(new BigDecimal("2850.00"), report.getNetSavings());
        assertEquals(1, report.getExpenseBreakdown().size());
        assertEquals("Food", report.getExpenseBreakdown().get(0).getCategoryName());
        assertEquals(new BigDecimal("150.00"), report.getExpenseBreakdown().get(0).getAmount());
    }

    @Test
    void exportTransactionsToCsv_Success() {
        // Report service now calls findAll(Specification) — mock that
        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(incomeTx, expenseTx));

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        reportService.exportTransactionsToCsv(email, pw);
        pw.flush();

        String csvOutput = sw.toString();
        
        // Assert header and values exist
        assertTrue(csvOutput.contains("ID,Date,Type,Category,Amount,Description"));
        assertTrue(csvOutput.contains("1,2026-07-05,INCOME,Salary,3000.00,\"Monthly Pay\""));
        assertTrue(csvOutput.contains("2,2026-07-10,EXPENSE,Food,150.00,\"Groceries\""));
    }
}

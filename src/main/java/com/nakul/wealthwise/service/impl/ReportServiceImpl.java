package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.response.CategorySummary;
import com.nakul.wealthwise.dto.response.MonthlyReportResponse;
import com.nakul.wealthwise.entity.Transaction;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.service.ReportService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;

    /** Build a JPA Specification for transactions, adding predicates only for non-null params. */
    private Specification<Transaction> txSpec(String email, LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("email"), email));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            if (endDate   != null) predicates.add(cb.lessThanOrEqualTo(root.get("date"),   endDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public MonthlyReportResponse getMonthlyReport(String email, Integer month, Integer year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate   = startDate.with(TemporalAdjusters.lastDayOfMonth());

        List<Transaction> transactions = transactionRepository.findAll(txSpec(email, startDate, endDate));

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        // Expense category summaries
        List<CategorySummary> expenseBreakdown = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> CategorySummary.builder().categoryName(entry.getKey()).amount(entry.getValue()).build())
                .collect(Collectors.toList());

        // Income category summaries
        List<CategorySummary> incomeBreakdown = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> CategorySummary.builder().categoryName(entry.getKey()).amount(entry.getValue()).build())
                .collect(Collectors.toList());

        return MonthlyReportResponse.builder()
                .month(month)
                .year(year)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .expenseBreakdown(expenseBreakdown)
                .incomeBreakdown(incomeBreakdown)
                .build();
    }

    @Override
    public void exportTransactionsToCsv(String email, PrintWriter writer) {
        List<Transaction> transactions = transactionRepository.findAll(txSpec(email, null, null));

        // Write CSV header
        writer.println("ID,Date,Type,Category,Amount,Description");

        for (Transaction t : transactions) {
            String categoryName = t.getCategory() != null ? t.getCategory().getName() : "N/A";
            String description  = t.getDescription() != null ? t.getDescription() : "";

            writer.println(String.format("%d,%s,%s,%s,%.2f,%s",
                    t.getId(),
                    t.getDate(),
                    t.getType(),
                    sanitizeCsvField(categoryName),
                    t.getAmount(),
                    sanitizeCsvField(description)
            ));
        }
    }

    /**
     * Prevents CSV formula injection (a.k.a. CSV injection / Excel macro injection).
     *
     * <p>Spreadsheet applications (Excel, LibreOffice Calc, Google Sheets) treat cells
     * that start with {@code =}, {@code +}, {@code -}, or {@code @} as formulas.
     * A malicious category name such as {@code =CMD|'/C calc'!A0} could execute
     * arbitrary commands when the victim opens the exported file.
     *
     * <p>The fix prefixes any such value with a tab character ({@code \t}), which
     * causes spreadsheets to treat it as plain text while keeping it human-readable.
     * The value is also always quoted so commas inside it don't break the CSV structure.
     */
    private String sanitizeCsvField(String value) {
        if (value == null || value.isEmpty()) return "\"\"";
        // Strip any leading dangerous formula characters
        String safe = value;
        if (safe.charAt(0) == '=' || safe.charAt(0) == '+' ||
            safe.charAt(0) == '-' || safe.charAt(0) == '@') {
            safe = "\t" + safe;
        }
        // Escape embedded double-quotes per RFC 4180
        safe = safe.replace("\"", "\"\"");
        return "\"" + safe + "\"";
    }
}

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
            String cleanDesc = t.getDescription() != null ? "\"" + t.getDescription().replace("\"", "\"\"") + "\"" : "";
            writer.println(String.format("%d,%s,%s,%s,%.2f,%s",
                    t.getId(),
                    t.getDate(),
                    t.getType(),
                    categoryName,
                    t.getAmount(),
                    cleanDesc
            ));
        }
    }
}

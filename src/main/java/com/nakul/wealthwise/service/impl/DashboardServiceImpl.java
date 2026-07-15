package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.response.*;
import com.nakul.wealthwise.entity.Budget;
import com.nakul.wealthwise.entity.Transaction;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.repository.BudgetRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.service.DashboardService;
import com.nakul.wealthwise.service.InvestmentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final InvestmentService investmentService;

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
    public DashboardResponse getDashboardData(String email) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year  = today.getYear();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate   = startDate.with(TemporalAdjusters.lastDayOfMonth());

        // 1. Current Month Cash Flow
        List<Transaction> currentMonthTxs = transactionRepository
                .findAll(txSpec(email, startDate, endDate));

        BigDecimal totalIncome = currentMonthTxs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = currentMonthTxs.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSavings = totalIncome.subtract(totalExpense);

        // 2. Portfolio Value
        List<InvestmentResponse> investments = investmentService.getPortfolio(email);
        BigDecimal portfolioValue = investments.stream()
                .map(InvestmentResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Net Worth (Total Cash All Time + Portfolio Value)
        List<Transaction> allTxs = transactionRepository
                .findAll(txSpec(email, null, null));

        BigDecimal allTimeIncome = allTxs.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allTimeExpense = allTxs.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netWorth = allTimeIncome.subtract(allTimeExpense).add(portfolioValue);

        // 4. Budgets
        List<Budget> currentBudgets = budgetRepository.findByUserEmailAndMonthAndYear(email, month, year);
        BigDecimal budgetLimit = currentBudgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal budgetSpent = BigDecimal.ZERO;
        for (Budget b : currentBudgets) {
            BigDecimal spent = transactionRepository.getSpentAmountByCategoryAndDateRange(
                    email, b.getCategory().getId(), startDate, endDate
            );
            if (spent != null) {
                budgetSpent = budgetSpent.add(spent);
            }
        }
        BigDecimal budgetRemaining = budgetLimit.subtract(budgetSpent);

        // 5. Category Distribution (Expenses)
        List<CategorySummary> categoryDistribution = currentMonthTxs.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(entry -> CategorySummary.builder().categoryName(entry.getKey()).amount(entry.getValue()).build())
                .collect(Collectors.toList());

        // 6. Historic Cash Flow (Past 6 Months)
        List<MonthlyCashFlow> monthlyCashFlows = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        for (int i = 5; i >= 0; i--) {
            LocalDate historicalDate = today.minusMonths(i);
            int m = historicalDate.getMonthValue();
            int y = historicalDate.getYear();

            LocalDate hStart = LocalDate.of(y, m, 1);
            LocalDate hEnd   = hStart.with(TemporalAdjusters.lastDayOfMonth());

            List<Transaction> hTxs = transactionRepository
                    .findAll(txSpec(email, hStart, hEnd));

            BigDecimal hIncome = hTxs.stream()
                    .filter(t -> t.getType() == TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal hExpense = hTxs.stream()
                    .filter(t -> t.getType() == TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyCashFlows.add(MonthlyCashFlow.builder()
                    .label(historicalDate.format(labelFormatter))
                    .income(hIncome)
                    .expense(hExpense)
                    .build());
        }

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .netWorth(netWorth)
                .portfolioValue(portfolioValue)
                .budgetLimit(budgetLimit)
                .budgetSpent(budgetSpent)
                .budgetRemaining(budgetRemaining)
                .categoryDistribution(categoryDistribution)
                .monthlyCashFlows(monthlyCashFlows)
                .build();
    }
}

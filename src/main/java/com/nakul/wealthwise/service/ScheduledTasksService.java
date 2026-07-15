package com.nakul.wealthwise.service;

import com.nakul.wealthwise.entity.Budget;
import com.nakul.wealthwise.repository.BudgetRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Evict the market prices cache every hour (3600000 ms) to ensure quotes are refreshed.
     */
    @Scheduled(fixedRate = 3600000)
    @CacheEvict(value = "marketPrices", allEntries = true)
    public void evictMarketPricesCache() {
        log.info("Scheduled task: Evicting all entries from marketPrices cache to force refresh.");
    }

    /**
     * Check budget consumption status daily at midnight.
     * Cron expression: "0 0 0 * * ?"
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void checkBudgetsDaily() {
        log.info("Scheduled task: Running daily budget threshold audit.");
        
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        List<Budget> activeBudgets = budgetRepository.findAll();
        for (Budget budget : activeBudgets) {
            if (budget.getMonth() == month && budget.getYear() == year) {
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

                BigDecimal spent = transactionRepository.getSpentAmountByCategoryAndDateRange(
                        budget.getUser().getEmail(),
                        budget.getCategory().getId(),
                        startDate,
                        endDate
                );

                if (spent != null) {
                    BigDecimal limit = budget.getAmount().multiply(new BigDecimal("0.90"));
                    if (spent.compareTo(budget.getAmount()) > 0) {
                        log.warn("BUDGET ALERT: User '{}' has EXCEEDED their budget for category '{}'. Budget: {}, Spent: {}",
                                budget.getUser().getEmail(), budget.getCategory().getName(), budget.getAmount(), spent);
                    } else if (spent.compareTo(limit) > 0) {
                        log.warn("BUDGET WARNING: User '{}' is close to exceeding their budget for category '{}' (> 90%). Budget: {}, Spent: {}",
                                budget.getUser().getEmail(), budget.getCategory().getName(), budget.getAmount(), spent);
                    }
                }
            }
        }
    }
}

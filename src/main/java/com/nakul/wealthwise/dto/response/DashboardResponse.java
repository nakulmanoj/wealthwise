package com.nakul.wealthwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private BigDecimal netWorth;
    private BigDecimal portfolioValue;
    private BigDecimal budgetLimit;
    private BigDecimal budgetSpent;
    private BigDecimal budgetRemaining;
    private List<CategorySummary> categoryDistribution;
    private List<MonthlyCashFlow> monthlyCashFlows;
}

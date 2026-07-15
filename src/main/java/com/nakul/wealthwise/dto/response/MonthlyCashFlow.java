package com.nakul.wealthwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCashFlow {
    private String label; // e.g. "Jul 2026"
    private BigDecimal income;
    private BigDecimal expense;
}

package com.nakul.wealthwise.dto.response;

import com.nakul.wealthwise.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDate date;
    private String description;
    private CategoryResponse category;
    private TransactionType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

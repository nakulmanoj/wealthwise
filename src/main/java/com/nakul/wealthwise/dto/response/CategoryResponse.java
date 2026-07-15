package com.nakul.wealthwise.dto.response;

import com.nakul.wealthwise.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private TransactionType type;
    private Boolean isDefault;
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.BudgetRequest;
import com.nakul.wealthwise.dto.response.BudgetResponse;
import java.util.List;

public interface BudgetService {
    BudgetResponse createBudget(String email, BudgetRequest request);
    BudgetResponse updateBudget(Long id, String email, BudgetRequest request);
    void deleteBudget(Long id, String email);
    BudgetResponse getBudgetById(Long id, String email);
    List<BudgetResponse> getBudgetsByMonthAndYear(String email, Integer month, Integer year);
}

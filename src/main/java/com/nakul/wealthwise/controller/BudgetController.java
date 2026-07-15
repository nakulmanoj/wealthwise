package com.nakul.wealthwise.controller;

import com.nakul.wealthwise.dto.request.BudgetRequest;
import com.nakul.wealthwise.dto.response.BudgetResponse;
import com.nakul.wealthwise.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request
    ) {
        BudgetResponse response = budgetService.createBudget(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request
    ) {
        return ResponseEntity.ok(budgetService.updateBudget(id, authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable Long id,
            Authentication authentication
    ) {
        budgetService.deleteBudget(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(budgetService.getBudgetById(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            Authentication authentication,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        List<BudgetResponse> budgets = budgetService.getBudgetsByMonthAndYear(authentication.getName(), month, year);
        return ResponseEntity.ok(budgets);
    }
}

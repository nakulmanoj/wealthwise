package com.nakul.wealthwise.controller;

import com.nakul.wealthwise.dto.request.InvestmentRequest;
import com.nakul.wealthwise.dto.response.InvestmentResponse;
import com.nakul.wealthwise.service.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @PostMapping
    public ResponseEntity<InvestmentResponse> addInvestment(
            Authentication authentication,
            @Valid @RequestBody InvestmentRequest request
    ) {
        InvestmentResponse response = investmentService.addOrUpdateInvestment(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvestmentResponse> updateInvestmentManual(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody InvestmentRequest request
    ) {
        return ResponseEntity.ok(investmentService.updateInvestmentManual(id, authentication.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(
            @PathVariable Long id,
            Authentication authentication
    ) {
        investmentService.deleteInvestment(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestmentResponse> getInvestmentById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(investmentService.getInvestmentById(id, authentication.getName()));
    }

    @GetMapping
    public ResponseEntity<List<InvestmentResponse>> getPortfolio(Authentication authentication) {
        return ResponseEntity.ok(investmentService.getPortfolio(authentication.getName()));
    }
}

package com.nakul.wealthwise.controller;

import com.nakul.wealthwise.dto.response.MonthlyReportResponse;
import com.nakul.wealthwise.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            Authentication authentication,
            @RequestParam Integer month,
            @RequestParam Integer year
    ) {
        MonthlyReportResponse report = reportService.getMonthlyReport(authentication.getName(), month, year);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export")
    public void exportToCsv(
            Authentication authentication,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");
        reportService.exportTransactionsToCsv(authentication.getName(), response.getWriter());
    }
}

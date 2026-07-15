package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.response.MonthlyReportResponse;
import java.io.PrintWriter;

public interface ReportService {
    MonthlyReportResponse getMonthlyReport(String email, Integer month, Integer year);
    void exportTransactionsToCsv(String email, PrintWriter writer);
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.InvestmentRequest;
import com.nakul.wealthwise.dto.response.InvestmentResponse;
import java.util.List;

public interface InvestmentService {
    InvestmentResponse addOrUpdateInvestment(String email, InvestmentRequest request);
    InvestmentResponse updateInvestmentManual(Long id, String email, InvestmentRequest request);
    void deleteInvestment(Long id, String email);
    InvestmentResponse getInvestmentById(Long id, String email);
    List<InvestmentResponse> getPortfolio(String email);
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.response.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboardData(String email);
}

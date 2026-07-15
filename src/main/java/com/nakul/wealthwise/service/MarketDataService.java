package com.nakul.wealthwise.service;

import java.math.BigDecimal;

public interface MarketDataService {
    BigDecimal getCurrentPrice(String symbol);
}

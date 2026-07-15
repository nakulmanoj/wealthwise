package com.nakul.wealthwise.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nakul.wealthwise.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    private final RestClient restClient;

    @Value("${application.market.finnhub.url:https://finnhub.io/api/v1}")
    private String finnhubUrl;

    @Value("${application.market.finnhub.token:demo}")
    private String finnhubToken;

    public MarketDataServiceImpl() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    @Cacheable(value = "marketPrices", key = "#symbol.toUpperCase()")
    public BigDecimal getCurrentPrice(String symbol) {
        String cleanSymbol = symbol.trim().toUpperCase();
        log.info("Fetching current price for symbol: {}", cleanSymbol);

        if ("DEMO".equalsIgnoreCase(finnhubToken) || finnhubToken.isBlank()) {
            log.info("Finnhub token not configured or demo mode. Using deterministic fallback mock price.");
            return generateDeterministicMockPrice(cleanSymbol);
        }

        try {
            FinnhubQuote quote = restClient.get()
                    .uri(finnhubUrl + "/quote?symbol=" + cleanSymbol + "&token=" + finnhubToken)
                    .retrieve()
                    .body(FinnhubQuote.class);

            if (quote != null && quote.currentPrice != null && quote.currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                return quote.currentPrice.setScale(2, RoundingMode.HALF_UP);
            } else {
                log.warn("Invalid pricing response from Finnhub for {}. Falling back to mock price.", cleanSymbol);
                return generateDeterministicMockPrice(cleanSymbol);
            }
        } catch (Exception e) {
            log.error("Error calling Finnhub API for symbol {}: {}. Falling back to mock price.", cleanSymbol, e.getMessage());
            return generateDeterministicMockPrice(cleanSymbol);
        }
    }

    private BigDecimal generateDeterministicMockPrice(String symbol) {
        int hash = Math.abs(symbol.hashCode());
        // Map symbol hash to a deterministic value between $10.00 and $500.00
        double base = 10.0 + (hash % 490);
        double cents = (hash % 100) / 100.0;
        return BigDecimal.valueOf(base + cents).setScale(2, RoundingMode.HALF_UP);
    }

    private static class FinnhubQuote {
        @JsonProperty("c")
        private BigDecimal currentPrice;
    }
}

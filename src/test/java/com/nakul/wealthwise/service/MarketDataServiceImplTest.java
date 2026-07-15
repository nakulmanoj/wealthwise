package com.nakul.wealthwise.service;

import com.nakul.wealthwise.service.impl.MarketDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceImplTest {

    @InjectMocks
    private MarketDataServiceImpl marketDataService;

    @Test
    void getCurrentPrice_DemoMode_ReturnsDeterministicMockPrice() {
        // Set properties using ReflectionTestUtils
        ReflectionTestUtils.setField(marketDataService, "finnhubToken", "demo");
        ReflectionTestUtils.setField(marketDataService, "finnhubUrl", "https://finnhub.io/api/v1");

        BigDecimal price1 = marketDataService.getCurrentPrice("AAPL");
        BigDecimal price2 = marketDataService.getCurrentPrice("AAPL");
        BigDecimal priceDifferent = marketDataService.getCurrentPrice("MSFT");

        assertNotNull(price1);
        assertTrue(price1.compareTo(BigDecimal.ZERO) > 0);
        
        // Assert determinism
        assertEquals(price1, price2);
        
        // Assert different symbol hashes lead to different mock prices
        assertNotEquals(price1, priceDifferent);
    }
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.InvestmentRequest;
import com.nakul.wealthwise.dto.response.InvestmentResponse;
import com.nakul.wealthwise.entity.Investment;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.InvestmentRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.impl.InvestmentServiceImpl;
import com.nakul.wealthwise.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceImplTest {

    @Mock
    private InvestmentRepository investmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MarketDataService marketDataService;

    @InjectMocks
    private InvestmentServiceImpl investmentService;

    private User user;
    private Investment investment;
    private String email = "test@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email(email)
                .build();

        investment = Investment.builder()
                .id(80L)
                .user(user)
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("10.0000"))
                .averageBuyPrice(new BigDecimal("150.00"))
                .build();

        lenient().when(marketDataService.getCurrentPrice(anyString())).thenReturn(new BigDecimal("150.00"));
    }

    @Test
    void addInvestment_NewSymbol_Success() {
        InvestmentRequest request = InvestmentRequest.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("10.0000"))
                .averageBuyPrice(new BigDecimal("150.00"))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(investmentRepository.findByUserEmailAndSymbol(email, "AAPL")).thenReturn(Optional.empty());
        when(investmentRepository.save(any(Investment.class))).thenReturn(investment);

        InvestmentResponse response = investmentService.addOrUpdateInvestment(email, request);

        assertNotNull(response);
        assertEquals("AAPL", response.getSymbol());
        assertEquals(new BigDecimal("150.00"), response.getAverageBuyPrice());
        assertEquals(new BigDecimal("10.0000"), response.getQuantity());
        verify(investmentRepository, times(1)).save(any(Investment.class));
    }

    @Test
    void addInvestment_ExistingSymbol_WeightedAverageCalculation() {
        InvestmentRequest request = InvestmentRequest.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("5.0000")) // Add 5 shares
                .averageBuyPrice(new BigDecimal("180.00")) // at $180.00
                .build();

        // Existing shares: 10 shares at $150.00. Total Cost = $1500
        // New shares: 5 shares at $180.00. Total Cost = $900
        // Total shares = 15
        // Weighted Average Buy Price = (1500 + 900) / 15 = 2400 / 15 = $160.00
        Investment expectedUpdated = Investment.builder()
                .id(80L)
                .user(user)
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("15.0000"))
                .averageBuyPrice(new BigDecimal("160.00"))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(investmentRepository.findByUserEmailAndSymbol(email, "AAPL")).thenReturn(Optional.of(investment));
        when(investmentRepository.save(any(Investment.class))).thenReturn(expectedUpdated);

        InvestmentResponse response = investmentService.addOrUpdateInvestment(email, request);

        assertNotNull(response);
        assertEquals("AAPL", response.getSymbol());
        assertEquals(0, new BigDecimal("160.00").compareTo(response.getAverageBuyPrice()));
        assertEquals(0, new BigDecimal("15.0000").compareTo(response.getQuantity()));
        verify(investmentRepository, times(1)).save(investment);
    }

    @Test
    void updateInvestmentManual_Success() {
        InvestmentRequest request = InvestmentRequest.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .quantity(new BigDecimal("12.5000"))
                .averageBuyPrice(new BigDecimal("155.00"))
                .build();

        when(investmentRepository.findByIdAndUserEmail(80L, email)).thenReturn(Optional.of(investment));
        when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvestmentResponse response = investmentService.updateInvestmentManual(80L, email, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("12.5000"), response.getQuantity());
        assertEquals(new BigDecimal("155.00"), response.getAverageBuyPrice());
    }

    @Test
    void deleteInvestment_Success() {
        when(investmentRepository.findByIdAndUserEmail(80L, email)).thenReturn(Optional.of(investment));

        investmentService.deleteInvestment(80L, email);

        verify(investmentRepository, times(1)).delete(investment);
    }

    @Test
    void getInvestmentById_Success() {
        when(investmentRepository.findByIdAndUserEmail(80L, email)).thenReturn(Optional.of(investment));

        InvestmentResponse response = investmentService.getInvestmentById(80L, email);

        assertNotNull(response);
        assertEquals(80L, response.getId());
    }

    @Test
    void getInvestmentById_ThrowsResourceNotFoundException() {
        when(investmentRepository.findByIdAndUserEmail(999L, email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> investmentService.getInvestmentById(999L, email));
    }

    @Test
    void getPortfolio_Success() {
        when(investmentRepository.findByUserEmail(email)).thenReturn(List.of(investment));

        List<InvestmentResponse> results = investmentService.getPortfolio(email);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(80L, results.get(0).getId());
    }
}

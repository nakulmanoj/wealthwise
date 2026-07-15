package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.request.InvestmentRequest;
import com.nakul.wealthwise.dto.response.InvestmentResponse;
import com.nakul.wealthwise.entity.Investment;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.InvestmentRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.InvestmentService;
import com.nakul.wealthwise.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;

    @Override
    public InvestmentResponse addOrUpdateInvestment(String email, InvestmentRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String symbol = request.getSymbol().trim().toUpperCase();
        Optional<Investment> existingOpt = investmentRepository.findByUserEmailAndSymbol(email, symbol);

        Investment investment;
        if (existingOpt.isPresent()) {
            // Weighted average cost basis calculation
            Investment existing = existingOpt.get();
            BigDecimal oldQuantity = existing.getQuantity();
            BigDecimal oldAveragePrice = existing.getAverageBuyPrice();

            BigDecimal addQuantity = request.getQuantity();
            BigDecimal addAveragePrice = request.getAverageBuyPrice();

            BigDecimal totalQuantity = oldQuantity.add(addQuantity);
            BigDecimal totalCost = oldQuantity.multiply(oldAveragePrice)
                    .add(addQuantity.multiply(addAveragePrice));

            BigDecimal newAveragePrice = totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP);

            existing.setQuantity(totalQuantity);
            existing.setAverageBuyPrice(newAveragePrice);
            existing.setName(request.getName()); // Update name in case it changed
            existing.setUpdatedAt(LocalDateTime.now());
            investment = investmentRepository.save(existing);
        } else {
            investment = Investment.builder()
                    .user(user)
                    .symbol(symbol)
                    .name(request.getName())
                    .quantity(request.getQuantity())
                    .averageBuyPrice(request.getAverageBuyPrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            investment = investmentRepository.save(investment);
        }

        return mapToResponse(investment);
    }

    @Override
    public InvestmentResponse updateInvestmentManual(Long id, String email, InvestmentRequest request) {
        Investment investment = investmentRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found or not accessible with ID: " + id));

        investment.setSymbol(request.getSymbol().trim().toUpperCase());
        investment.setName(request.getName());
        investment.setQuantity(request.getQuantity());
        investment.setAverageBuyPrice(request.getAverageBuyPrice());
        investment.setUpdatedAt(LocalDateTime.now());

        Investment updated = investmentRepository.save(investment);
        return mapToResponse(updated);
    }

    @Override
    public void deleteInvestment(Long id, String email) {
        Investment investment = investmentRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found or not accessible with ID: " + id));

        investmentRepository.delete(investment);
    }

    @Override
    public InvestmentResponse getInvestmentById(Long id, String email) {
        Investment investment = investmentRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found or not accessible with ID: " + id));
        return mapToResponse(investment);
    }

    @Override
    public List<InvestmentResponse> getPortfolio(String email) {
        return investmentRepository.findByUserEmail(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InvestmentResponse mapToResponse(Investment investment) {
        BigDecimal currentPrice = marketDataService.getCurrentPrice(investment.getSymbol()); 

        BigDecimal totalCost = investment.getQuantity().multiply(investment.getAverageBuyPrice());
        BigDecimal currentValue = investment.getQuantity().multiply(currentPrice);
        BigDecimal profitLoss = currentValue.subtract(totalCost);
        
        BigDecimal profitLossPercentage = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercentage = profitLoss.divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return InvestmentResponse.builder()
                .id(investment.getId())
                .symbol(investment.getSymbol())
                .name(investment.getName())
                .quantity(investment.getQuantity())
                .averageBuyPrice(investment.getAverageBuyPrice())
                .currentPrice(currentPrice)
                .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .currentValue(currentValue.setScale(2, RoundingMode.HALF_UP))
                .profitLoss(profitLoss.setScale(2, RoundingMode.HALF_UP))
                .profitLossPercentage(profitLossPercentage.setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}

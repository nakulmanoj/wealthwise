package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.TransactionRequest;
import com.nakul.wealthwise.dto.response.TransactionResponse;
import com.nakul.wealthwise.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransactionService {
    TransactionResponse createTransaction(String email, TransactionRequest request);
    TransactionResponse updateTransaction(Long id, String email, TransactionRequest request);
    void deleteTransaction(Long id, String email);
    TransactionResponse getTransactionById(Long id, String email);
    Page<TransactionResponse> searchTransactions(
            String email,
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            String description,
            Pageable pageable
    );
}

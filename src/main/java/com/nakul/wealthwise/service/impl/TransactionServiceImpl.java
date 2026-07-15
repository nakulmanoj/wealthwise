package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.request.TransactionRequest;
import com.nakul.wealthwise.dto.response.CategoryResponse;
import com.nakul.wealthwise.dto.response.TransactionResponse;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.Transaction;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.CategoryRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.TransactionService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public TransactionResponse createTransaction(String email, TransactionRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Category category = categoryRepository.findVisibleByIdAndUser(request.getCategoryId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible with ID: " + request.getCategoryId()));

        if (category.getType() != request.getType()) {
            throw new IllegalArgumentException("Transaction type (" + request.getType() + ") does not match the Category type (" + category.getType() + ")");
        }

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .date(request.getDate())
                .description(request.getDescription())
                .category(category)
                .user(user)
                .type(request.getType())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    @Override
    public TransactionResponse updateTransaction(Long id, String email, TransactionRequest request) {
        Transaction transaction = transactionRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible with ID: " + id));

        Category category = categoryRepository.findVisibleByIdAndUser(request.getCategoryId(), email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or not accessible with ID: " + request.getCategoryId()));

        if (category.getType() != request.getType()) {
            throw new IllegalArgumentException("Transaction type (" + request.getType() + ") does not match the Category type (" + category.getType() + ")");
        }

        transaction.setAmount(request.getAmount());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        transaction.setCategory(category);
        transaction.setType(request.getType());
        transaction.setUpdatedAt(LocalDateTime.now());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        return mapToResponse(updatedTransaction);
    }

    @Override
    public void deleteTransaction(Long id, String email) {
        Transaction transaction = transactionRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible with ID: " + id));

        transactionRepository.delete(transaction);
    }

    @Override
    public TransactionResponse getTransactionById(Long id, String email) {
        Transaction transaction = transactionRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found or not accessible with ID: " + id));
        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> searchTransactions(
            String email,
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long categoryId,
            String description,
            Pageable pageable
    ) {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by authenticated user's email
            predicates.add(cb.equal(root.get("user").get("email"), email));

            // Only add each condition when the parameter is non-null — avoids
            // PostgreSQL "could not determine data type of parameter" error
            // that occurs with JPQL IS NULL checks on enum-typed parameters
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), endDate));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (description != null && !description.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + description.toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        CategoryResponse categoryResponse = null;
        if (transaction.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(transaction.getCategory().getId())
                    .name(transaction.getCategory().getName())
                    .type(transaction.getCategory().getType())
                    .isDefault(transaction.getCategory().getIsDefault())
                    .build();
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .category(categoryResponse)
                .type(transaction.getType())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}

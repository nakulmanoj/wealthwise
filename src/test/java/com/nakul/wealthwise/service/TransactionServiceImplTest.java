package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.TransactionRequest;
import com.nakul.wealthwise.dto.response.TransactionResponse;
import com.nakul.wealthwise.entity.*;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.CategoryRepository;
import com.nakul.wealthwise.repository.TransactionRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User user;
    private Category category;
    private Transaction transaction;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email(email).build();

        category = Category.builder()
                .id(10L).name("Salary")
                .type(TransactionType.INCOME).isDefault(true)
                .build();

        transaction = Transaction.builder()
                .id(100L)
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2026, 7, 15))
                .description("Monthly Paycheck")
                .category(category).user(user)
                .type(TransactionType.INCOME)
                .build();
    }

    @Test
    void createTransaction_Success() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .date(LocalDate.of(2026, 7, 15))
                .description("Monthly Paycheck")
                .categoryId(10L).type(TransactionType.INCOME)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse response = transactionService.createTransaction(email, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.00"), response.getAmount());
        assertEquals("Salary", response.getCategory().getName());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_ThrowsIllegalArgumentExceptionForTypeMismatch() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("50.00"))
                .date(LocalDate.of(2026, 7, 15))
                .description("Grocery Store")
                .categoryId(10L) // INCOME category
                .type(TransactionType.EXPENSE) // but EXPENSE type
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(email, request));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionById_Success() {
        when(transactionRepository.findByIdAndUserEmail(100L, email)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(100L, email);

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    void getTransactionById_ThrowsResourceNotFoundException() {
        when(transactionRepository.findByIdAndUserEmail(999L, email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.getTransactionById(999L, email));
    }

    @Test
    void updateTransaction_Success() {
        TransactionRequest request = TransactionRequest.builder()
                .amount(new BigDecimal("5500.00"))
                .date(LocalDate.of(2026, 7, 15))
                .description("Updated paycheck")
                .categoryId(10L).type(TransactionType.INCOME)
                .build();

        when(transactionRepository.findByIdAndUserEmail(100L, email)).thenReturn(Optional.of(transaction));
        when(categoryRepository.findVisibleByIdAndUser(10L, email)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(100L, email, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("5500.00"), response.getAmount());
        assertEquals("Updated paycheck", response.getDescription());
    }

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.findByIdAndUserEmail(100L, email)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(100L, email);

        verify(transactionRepository, times(1)).delete(transaction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchTransactions_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(transaction));

        // Service now calls findAll(Specification, Pageable) — mock that
        when(transactionRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        Page<TransactionResponse> result = transactionService.searchTransactions(
                email, null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(100L, result.getContent().get(0).getId());
    }
}

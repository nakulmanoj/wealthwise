package com.nakul.wealthwise.repository;

import com.nakul.wealthwise.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT t FROM Transaction t WHERE t.id = :id AND t.user.email = :email")
    Optional<Transaction> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.email = :email " +
           "AND t.category.id = :categoryId " +
           "AND t.type = 'EXPENSE' " +
           "AND t.date >= :startDate AND t.date <= :endDate")
    BigDecimal getSpentAmountByCategoryAndDateRange(
            @Param("email") String email,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

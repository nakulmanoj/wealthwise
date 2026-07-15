package com.nakul.wealthwise.repository;

import com.nakul.wealthwise.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("SELECT b FROM Budget b WHERE b.id = :id AND b.user.email = :email")
    Optional<Budget> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    @Query("SELECT b FROM Budget b WHERE b.user.email = :email AND b.month = :month AND b.year = :year")
    List<Budget> findByUserEmailAndMonthAndYear(@Param("email") String email, @Param("month") Integer month, @Param("year") Integer year);

    @Query("SELECT b FROM Budget b WHERE b.user.email = :email AND b.category.id = :categoryId AND b.month = :month AND b.year = :year")
    Optional<Budget> findByUserEmailAndCategoryIdAndMonthAndYear(
            @Param("email") String email,
            @Param("categoryId") Long categoryId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

    @Query("SELECT COUNT(b) > 0 FROM Budget b WHERE b.user.email = :email AND b.category.id = :categoryId AND b.month = :month AND b.year = :year")
    boolean existsByUserEmailAndCategoryIdAndMonthAndYear(
            @Param("email") String email,
            @Param("categoryId") Long categoryId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );
}

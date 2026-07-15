package com.nakul.wealthwise.repository;

import com.nakul.wealthwise.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    @Query("SELECT i FROM Investment i WHERE i.id = :id AND i.user.email = :email")
    Optional<Investment> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);

    @Query("SELECT i FROM Investment i WHERE i.user.email = :email")
    List<Investment> findByUserEmail(@Param("email") String email);

    @Query("SELECT i FROM Investment i WHERE i.user.email = :email AND LOWER(i.symbol) = LOWER(:symbol)")
    Optional<Investment> findByUserEmailAndSymbol(@Param("email") String email, @Param("symbol") String symbol);

    @Query("SELECT COUNT(i) > 0 FROM Investment i WHERE i.user.email = :email AND LOWER(i.symbol) = LOWER(:symbol)")
    boolean existsByUserEmailAndSymbol(@Param("email") String email, @Param("symbol") String symbol);
}

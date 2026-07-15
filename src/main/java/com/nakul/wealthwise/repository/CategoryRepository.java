package com.nakul.wealthwise.repository;

import com.nakul.wealthwise.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.isDefault = true OR (c.user.email = :email)")
    List<Category> findAllVisibleToUser(@Param("email") String email);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.isDefault = true OR (c.user.email = :email))")
    Optional<Category> findVisibleByIdAndUser(@Param("id") Long id, @Param("email") String email);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.isDefault = true OR (c.user.email = :email))")
    boolean existsByNameIgnoreCaseAndUserOrIsDefault(@Param("name") String name, @Param("email") String email);
}

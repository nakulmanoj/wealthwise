package com.nakul.wealthwise.config;

import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultCategories();
    }

    private void initializeDefaultCategories() {
        // List of standard default categories
        List<Category> defaultCategories = List.of(
                createDefaultCategory("Salary", TransactionType.INCOME),
                createDefaultCategory("Investments", TransactionType.INCOME),
                createDefaultCategory("Other Income", TransactionType.INCOME),
                createDefaultCategory("Food & Dining", TransactionType.EXPENSE),
                createDefaultCategory("Rent & Bills", TransactionType.EXPENSE),
                createDefaultCategory("Shopping", TransactionType.EXPENSE),
                createDefaultCategory("Travel & Transport", TransactionType.EXPENSE),
                createDefaultCategory("Entertainment", TransactionType.EXPENSE),
                createDefaultCategory("Miscellaneous", TransactionType.EXPENSE)
        );

        for (Category defaultCat : defaultCategories) {
            // Check if it already exists as a default category to avoid duplicates
            boolean exists = categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault(defaultCat.getName(), "system");
            if (!exists) {
                categoryRepository.save(defaultCat);
            }
        }
    }

    private Category createDefaultCategory(String name, TransactionType type) {
        return Category.builder()
                .name(name)
                .type(type)
                .isDefault(true)
                .user(null) // system-owned
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

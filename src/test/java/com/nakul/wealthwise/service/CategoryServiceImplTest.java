package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.CategoryRequest;
import com.nakul.wealthwise.dto.response.CategoryResponse;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.TransactionType;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.CategoryAlreadyExistsException;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.CategoryRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private User user;
    private Category defaultCategory;
    private Category customCategory;
    private String email = "test@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email(email)
                .build();

        defaultCategory = Category.builder()
                .id(1L)
                .name("Salary")
                .type(TransactionType.INCOME)
                .isDefault(true)
                .build();

        customCategory = Category.builder()
                .id(2L)
                .name("Special Subscriptions")
                .type(TransactionType.EXPENSE)
                .isDefault(false)
                .user(user)
                .build();
    }

    @Test
    void getAllCategories_Success() {
        when(categoryRepository.findAllVisibleToUser(email)).thenReturn(List.of(defaultCategory, customCategory));

        List<CategoryResponse> results = categoryService.getAllCategories(email);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Salary", results.get(0).getName());
        assertTrue(results.get(0).getIsDefault());
        assertEquals("Special Subscriptions", results.get(1).getName());
        assertFalse(results.get(1).getIsDefault());
    }

    @Test
    void getCategoryById_Success() {
        when(categoryRepository.findVisibleByIdAndUser(2L, email)).thenReturn(Optional.of(customCategory));

        CategoryResponse result = categoryService.getCategoryById(2L, email);

        assertNotNull(result);
        assertEquals("Special Subscriptions", result.getName());
    }

    @Test
    void getCategoryById_NotFound() {
        when(categoryRepository.findVisibleByIdAndUser(99L, email)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(99L, email));
    }

    @Test
    void createCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Gifts")
                .type(TransactionType.INCOME)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault("Gifts", email)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        CategoryResponse result = categoryService.createCategory(email, request);

        assertNotNull(result);
        assertEquals("Gifts", result.getName());
        assertEquals(3L, result.getId());
        assertFalse(result.getIsDefault());
    }

    @Test
    void createCategory_ThrowsCategoryAlreadyExistsException() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Salary")
                .type(TransactionType.INCOME)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault("Salary", email)).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryService.createCategory(email, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_Success() {
        CategoryRequest request = CategoryRequest.builder()
                .name("Subscriptions")
                .type(TransactionType.EXPENSE)
                .build();

        when(categoryRepository.findVisibleByIdAndUser(2L, email)).thenReturn(Optional.of(customCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault("Subscriptions", email)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.updateCategory(2L, email, request);

        assertNotNull(result);
        assertEquals("Subscriptions", result.getName());
        verify(categoryRepository, times(1)).save(customCategory);
    }

    @Test
    void updateCategory_ThrowsIllegalArgumentExceptionWhenDefaultCategory() {
        CategoryRequest request = CategoryRequest.builder()
                .name("New Salary")
                .type(TransactionType.INCOME)
                .build();

        when(categoryRepository.findVisibleByIdAndUser(1L, email)).thenReturn(Optional.of(defaultCategory));

        assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(1L, email, request));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_Success() {
        when(categoryRepository.findVisibleByIdAndUser(2L, email)).thenReturn(Optional.of(customCategory));

        categoryService.deleteCategory(2L, email);

        verify(categoryRepository, times(1)).delete(customCategory);
    }

    @Test
    void deleteCategory_ThrowsIllegalArgumentExceptionWhenDefaultCategory() {
        when(categoryRepository.findVisibleByIdAndUser(1L, email)).thenReturn(Optional.of(defaultCategory));

        assertThrows(IllegalArgumentException.class, () -> categoryService.deleteCategory(1L, email));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}

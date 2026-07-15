package com.nakul.wealthwise.service.impl;

import com.nakul.wealthwise.dto.request.CategoryRequest;
import com.nakul.wealthwise.dto.response.CategoryResponse;
import com.nakul.wealthwise.entity.Category;
import com.nakul.wealthwise.entity.User;
import com.nakul.wealthwise.exception.CategoryAlreadyExistsException;
import com.nakul.wealthwise.exception.ResourceNotFoundException;
import com.nakul.wealthwise.repository.CategoryRepository;
import com.nakul.wealthwise.repository.UserRepository;
import com.nakul.wealthwise.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public List<CategoryResponse> getAllCategories(String email) {
        return categoryRepository.findAllVisibleToUser(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long id, String email) {
        Category category = categoryRepository.findVisibleByIdAndUser(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    @Override
    public CategoryResponse createCategory(String email, CategoryRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault(request.getName(), email)) {
            throw new CategoryAlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .type(request.getType())
                .isDefault(false)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponse updateCategory(Long id, String email, CategoryRequest request) {
        Category category = categoryRepository.findVisibleByIdAndUser(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new IllegalArgumentException("Cannot modify default system categories");
        }

        // Check if updating to a name that already exists for this user (excluding current category ID)
        if (categoryRepository.existsByNameIgnoreCaseAndUserOrIsDefault(request.getName(), email)
                && !category.getName().equalsIgnoreCase(request.getName())) {
            throw new CategoryAlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setType(request.getType());
        category.setUpdatedAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(Long id, String email) {
        Category category = categoryRepository.findVisibleByIdAndUser(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete default system categories");
        }

        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .isDefault(category.getIsDefault())
                .build();
    }
}

package com.nakul.wealthwise.service;

import com.nakul.wealthwise.dto.request.CategoryRequest;
import com.nakul.wealthwise.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(String email);
    CategoryResponse getCategoryById(Long id, String email);
    CategoryResponse createCategory(String email, CategoryRequest request);
    CategoryResponse updateCategory(Long id, String email, CategoryRequest request);
    void deleteCategory(Long id, String email);
}

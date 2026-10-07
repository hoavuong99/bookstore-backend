package com.example.bookstore.service;

import com.example.bookstore.dto.category.CategoryRequest;
import com.example.bookstore.dto.category.CategoryResponse;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile);

    List<CategoryResponse> getAllCategories();
    Page<CategoryResponse> getAllCategories(Pageable pageable);

    CategoryResponse getCategoryById(Long id);

    CategoryResponse updateCategory(Long id, CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request, MultipartFile imageFile);

    void deleteCategory(Long id);
}

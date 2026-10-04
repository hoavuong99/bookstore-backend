package com.example.bookstore.service.impl;

import com.example.bookstore.dto.category.CategoryRequest;
import com.example.bookstore.dto.category.CategoryResponse;
import com.example.bookstore.entity.Category;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.CategoryAlreadyExistsException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CategoryAlreadyExistsException("Category already exists with name: " + normalizedName);
        }

        Category category = new Category();
        applyRequest(category, request);
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(findCategoryById(id));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryById(id);
        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new CategoryAlreadyExistsException("Category already exists with name: " + normalizedName);
        }

        applyRequest(category, request);
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryById(id);
        if (bookRepository.existsByCategories_Id(id)) {
            throw new BadRequestException("Cannot delete category because it is being used by one or more books");
        }
        categoryRepository.delete(category);
    }

    private Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.getName().trim());
        if (request.getDescription() == null) {
            category.setDescription(null);
            return;
        }

        String normalizedDescription = request.getDescription().trim();
        category.setDescription(normalizedDescription.isEmpty() ? null : normalizedDescription);
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}

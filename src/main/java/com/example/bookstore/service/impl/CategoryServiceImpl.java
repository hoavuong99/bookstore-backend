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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @Value("${app.upload-dir:uploads/books}")
    private String uploadDir;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        return createCategory(request, null);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile imageFile) {
        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new CategoryAlreadyExistsException("Category already exists with name: " + normalizedName);
        }

        Category category = new Category();
        applyRequest(category, request);
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImageUrl(storeImage(imageFile));
        }
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
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return mapToResponse(findCategoryById(id));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        return updateCategory(id, request, null);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, MultipartFile imageFile) {
        Category category = findCategoryById(id);
        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new CategoryAlreadyExistsException("Category already exists with name: " + normalizedName);
        }

        applyRequest(category, request);
        if (imageFile != null && !imageFile.isEmpty()) {
            category.setImageUrl(storeImage(imageFile));
        }
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
        } else {
            String normalizedDescription = request.getDescription().trim();
            category.setDescription(normalizedDescription.isEmpty() ? null : normalizedDescription);
        }

        if (request.getImageUrl() == null || request.getImageUrl().isBlank()) {
            category.setImageUrl(null);
        } else {
            category.setImageUrl(request.getImageUrl().trim());
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .build();
    }

    private String storeImage(MultipartFile imageFile) {
        validateImage(imageFile);
        String originalFilename = StringUtils.cleanPath(
                imageFile.getOriginalFilename() == null ? "" : imageFile.getOriginalFilename()
        );
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("categories");
        try {
            Files.createDirectories(uploadPath);
            String storedFileName = UUID.randomUUID() + extension;
            imageFile.transferTo(uploadPath.resolve(storedFileName));
            return "/uploads/categories/" + storedFileName;
        } catch (IOException exception) {
            throw new BadRequestException("Failed to upload category image file");
        }
    }

    private void validateImage(MultipartFile imageFile) {
        if (imageFile.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Image file must be 5 MB or smaller");
        }
        String contentType = imageFile.getContentType();
        if (contentType == null || !List.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG, WEBP, and GIF images are supported");
        }
    }
}

package com.example.bookstore.service.impl;

import com.example.bookstore.dto.book.BookRequest;
import com.example.bookstore.dto.book.BookResponse;
import com.example.bookstore.service.BookService;
import com.example.bookstore.exception.BookAlreadyExistsException;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Category;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.dto.dashboard.BestSellerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.upload-dir:uploads/books}")
    private String uploadDir;

    @Override
    @Transactional
    public BookResponse createBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn().trim())) {
            throw new BookAlreadyExistsException("Book already exists with ISBN: " + request.getIsbn().trim());
        }

        Book book = new Book();
        applyRequest(book, request);
        Book savedBook = bookRepository.save(book);
        return mapToResponse(savedBook);
    }

    @Override
    @Transactional
    public BookResponse createBook(BookRequest request, MultipartFile imageFile) {
        if (bookRepository.existsByIsbn(request.getIsbn().trim())) {
            throw new BookAlreadyExistsException("Book already exists with ISBN: " + request.getIsbn().trim());
        }

        Book book = new Book();
        applyRequest(book, request);

        if (imageFile != null && !imageFile.isEmpty()) {
            book.setImageUrl(storeImage(imageFile));
        }

        Book savedBook = bookRepository.save(book);
        return mapToResponse(savedBook);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        Pageable newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return bookRepository.findAll(newestFirst)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(String search, Long categoryId, BigDecimal maxPrice, Pageable pageable) {
        String normalizedSearch = search == null ? null : search.trim();
        Pageable newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return bookRepository.searchBooks(
                        normalizedSearch,
                        categoryId,
                        maxPrice,
                        newestFirst
                )
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getBestSellingBooks(Pageable pageable) {
        List<BestSellerResponse> bestSellers = orderItemRepository.findBestSellers(pageable);
        Map<Long, Book> booksById = new HashMap<>();
        bookRepository.findAllById(bestSellers.stream().map(BestSellerResponse::getBookId).toList())
                .forEach(book -> booksById.put(book.getId(), book));

        return bestSellers.stream()
                .map(bestSeller -> booksById.get(bestSeller.getBookId()))
                .filter(Objects::nonNull)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getLatestBooks(Pageable pageable) {
        return bookRepository.findAllByOrderByCreatedAtDesc(pageable).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getEditorsPicks(Pageable pageable) {
        return bookRepository.findByEditorsPickTrueOrderByCreatedAtDesc(pageable).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = findBookById(id);
        return mapToResponse(book);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        return updateBook(id, request, null);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long id, BookRequest request, MultipartFile imageFile) {
        Book book = findBookById(id);

        String normalizedIsbn = request.getIsbn().trim();
        if (bookRepository.existsByIsbnAndIdNot(normalizedIsbn, id)) {
            throw new BookAlreadyExistsException("Book already exists with ISBN: " + normalizedIsbn);
        }

        applyRequest(book, request);
        if (imageFile != null && !imageFile.isEmpty()) {
            book.setImageUrl(storeImage(imageFile));
        }
        Book savedBook = bookRepository.save(book);
        return mapToResponse(savedBook);
    }

    @Override
    @Transactional
    public void deleteBook(Long id) {
        Book book = findBookById(id);
        bookRepository.delete(book);
    }

    private Book findBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private void applyRequest(Book book, BookRequest request) {
        Set<Long> requestedCategoryIds = request.getCategoryIds().stream()
            .filter(Objects::nonNull)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<Category> categories = categoryRepository.findAllById(requestedCategoryIds);
        if (categories.size() != requestedCategoryIds.size()) {
            Set<Long> foundCategoryIds = categories.stream()
                .map(Category::getId)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

            List<Long> missingCategoryIds = requestedCategoryIds.stream()
                .filter(id -> !foundCategoryIds.contains(id))
                .toList();

            throw new ResourceNotFoundException("Category not found with id(s): " + missingCategoryIds);
        }

        book.setTitle(request.getTitle().trim());
        if (request.getAuthorName() == null || request.getAuthorName().isBlank()) {
            book.setAuthorName(null);
        } else {
            book.setAuthorName(request.getAuthorName().trim());
        }
        book.setIsbn(request.getIsbn().trim());
        book.setPrice(request.getPrice());
        book.setStockQuantity(request.getStockQuantity());
        book.setCategories(new HashSet<>(categories));
        if (request.getDescription() == null) {
            book.setDescription(null);
        } else {
            String normalizedDescription = request.getDescription().trim();
            book.setDescription(normalizedDescription.isEmpty() ? null : normalizedDescription);
        }

        if (request.getImageUrl() != null) {
            String normalizedImageUrl = request.getImageUrl().trim();
            book.setImageUrl(normalizedImageUrl.isEmpty() ? null : normalizedImageUrl);
        }

        book.setRating(request.getRating());
        book.setEditorsPick(Boolean.TRUE.equals(request.getEditorsPick()));
    }

    private String storeImage(MultipartFile imageFile) {
        validateImage(imageFile);
        String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename() == null ? "" : imageFile.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storedFileName = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
            Path targetFile = uploadPath.resolve(storedFileName);
            imageFile.transferTo(targetFile);
            return "/uploads/books/" + storedFileName;
        } catch (IOException exception) {
            throw new BadRequestException("Failed to upload image file");
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

    private BookResponse mapToResponse(Book book) {
        List<Category> sortedCategories = new ArrayList<>(book.getCategories());
        sortedCategories.sort(Comparator.comparing(Category::getId));

        List<Long> categoryIds = sortedCategories.stream()
            .map(Category::getId)
            .toList();

        List<String> categoryNames = sortedCategories.stream()
            .map(Category::getName)
            .toList();

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .stockQuantity(book.getStockQuantity())
            .categoryIds(categoryIds)
            .categoryNames(categoryNames)
                .description(book.getDescription())
                .imageUrl(book.getImageUrl())
                .rating(book.getRating())
                .editorsPick(book.getEditorsPick())
                .build();
    }
}

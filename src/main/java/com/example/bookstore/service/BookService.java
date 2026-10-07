package com.example.bookstore.service;

import com.example.bookstore.dto.book.BookRequest;
import com.example.bookstore.dto.book.BookResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.math.BigDecimal;

public interface BookService {
    BookResponse createBook(BookRequest request);

    BookResponse createBook(BookRequest request, MultipartFile imageFile);

    List<BookResponse> getAllBooks();
    Page<BookResponse> getAllBooks(Pageable pageable);
    Page<BookResponse> getAllBooks(String search, Long categoryId, BigDecimal maxPrice, Pageable pageable);
    List<BookResponse> getBestSellingBooks(Pageable pageable);
    List<BookResponse> getLatestBooks(Pageable pageable);
    List<BookResponse> getEditorsPicks(Pageable pageable);

    BookResponse getBookById(Long id);

    BookResponse updateBook(Long id, BookRequest request);
    BookResponse updateBook(Long id, BookRequest request, MultipartFile imageFile);

    void deleteBook(Long id);
}

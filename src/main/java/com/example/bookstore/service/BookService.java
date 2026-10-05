package com.example.bookstore.service;

import com.example.bookstore.dto.book.BookRequest;
import com.example.bookstore.dto.book.BookResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookService {
    BookResponse createBook(BookRequest request);

    BookResponse createBook(BookRequest request, MultipartFile imageFile);

    List<BookResponse> getAllBooks();
    Page<BookResponse> getAllBooks(Pageable pageable);
    Page<BookResponse> getAllBooks(String search, Pageable pageable);
    List<BookResponse> getBestSellingBooks(Pageable pageable);

    BookResponse getBookById(Long id);

    BookResponse updateBook(Long id, BookRequest request);

    void deleteBook(Long id);
}

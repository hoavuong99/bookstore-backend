package com.example.bookstore.service;

import com.example.bookstore.dto.book.BookRequest;
import com.example.bookstore.dto.book.BookResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {
    BookResponse createBook(BookRequest request);

    BookResponse createBook(BookRequest request, MultipartFile imageFile);

    List<BookResponse> getAllBooks();

    BookResponse getBookById(Long id);

    BookResponse updateBook(Long id, BookRequest request);

    void deleteBook(Long id);
}

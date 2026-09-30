package com.example.bookstore.dto.book;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BookResponse {
    private Long id;
    private String title;
    private String isbn;
    private BigDecimal price;
    private Integer stockQuantity;
    private String description;
    private String imageUrl;
}

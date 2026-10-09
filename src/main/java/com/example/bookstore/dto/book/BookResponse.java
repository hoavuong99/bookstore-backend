package com.example.bookstore.dto.book;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BookResponse {
    private Long id;
    private String title;
    private String authorName;
    private String isbn;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<Long> categoryIds;
    private List<String> categoryNames;
    private String description;
    private String imageUrl;
    private Boolean editorsPick;
}

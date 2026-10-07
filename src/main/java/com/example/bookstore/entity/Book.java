package com.example.bookstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "books")
public class Book extends BaseEntity {

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "author_name", columnDefinition = "NVARCHAR(255)")
    private String authorName;

    @Column(nullable = false, unique = true, columnDefinition = "NVARCHAR(255)")
    private String isbn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "editors_pick", nullable = false)
    private Boolean editorsPick = false;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "book_category",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
}
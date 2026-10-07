package com.example.bookstore.dto.book;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BookRequest {

    @NotBlank
    private String title;

    @Size(max = 255)
    private String authorName;

    @NotBlank
    private String isbn;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotEmpty
    private List<@NotNull Long> categoryIds;

    @Size(max = 5000)
    private String description;

    private String imageUrl;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    private BigDecimal rating;

    private Boolean editorsPick;
}

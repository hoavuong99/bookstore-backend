package com.example.bookstore.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank
    private String name;

    @Size(max = 5000)
    private String description;

    @Size(max = 2048)
    private String imageUrl;
}

package com.example.bookstore.dto.review;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewEligibilityResponse {
    private boolean eligible;
    private boolean reviewed;
}

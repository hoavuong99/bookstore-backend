package com.example.bookstore.controller;

import com.example.bookstore.common.ApiResponse;
import com.example.bookstore.dto.review.ReviewEligibilityResponse;
import com.example.bookstore.dto.review.ReviewRequest;
import com.example.bookstore.dto.review.ReviewResponse;
import com.example.bookstore.security.CustomUserPrincipal;
import com.example.bookstore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviews(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", reviewService.getReviews(bookId)));
    }

    @GetMapping("/eligibility")
    public ResponseEntity<ApiResponse<ReviewEligibilityResponse>> getEligibility(
            @PathVariable Long bookId,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                "Review eligibility retrieved successfully",
                reviewService.getEligibility(principal.getId(), bookId)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Review created successfully",
                reviewService.createReview(principal.getId(), bookId, request)
        ));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                "Review updated successfully",
                reviewService.updateReview(principal.getId(), reviewId, request)
        ));
    }
}

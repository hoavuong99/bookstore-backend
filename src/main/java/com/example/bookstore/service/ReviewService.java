package com.example.bookstore.service;

import com.example.bookstore.dto.review.ReviewEligibilityResponse;
import com.example.bookstore.dto.review.ReviewRequest;
import com.example.bookstore.dto.review.ReviewResponse;

import java.util.List;

public interface ReviewService {
    List<ReviewResponse> getReviews(Long bookId);

    ReviewEligibilityResponse getEligibility(Long userId, Long bookId);

    ReviewResponse createReview(Long userId, Long bookId, ReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);
}

package com.example.bookstore.service.impl;

import com.example.bookstore.dto.review.ReviewEligibilityResponse;
import com.example.bookstore.dto.review.ReviewRequest;
import com.example.bookstore.dto.review.ReviewResponse;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Review;
import com.example.bookstore.entity.User;
import com.example.bookstore.enums.Role;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.repository.ReviewRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(Long bookId) {
        ensureBookExists(bookId);
        return reviewRepository.findByBookIdOrderByCreatedAtDesc(bookId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse getEligibility(Long userId, Long bookId) {
        ensureBookExists(bookId);
        User user = getUser(userId);
        return ReviewEligibilityResponse.builder()
                .eligible(user.getRole() == Role.CUSTOMER
                        && orderItemRepository.countDeliveredByUserAndBook(userId, bookId) > 0)
                .reviewed(reviewRepository.existsByUserIdAndBookId(userId, bookId))
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, Long bookId, ReviewRequest request) {
        Book book = ensureBookExists(bookId);
        User user = getUser(userId);
        if (user.getRole() != Role.CUSTOMER
                || orderItemRepository.countDeliveredByUserAndBook(userId, bookId) == 0) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sách sau khi đơn hàng đã giao thành công");
        }
        if (reviewRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new BadRequestException("Bạn đã đánh giá sách này");
        }

        Review review = new Review();
        review.setUser(user);
        review.setBook(book);
        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        return mapToResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own review");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment().trim());
        return mapToResponse(reviewRepository.save(review));
    }

    private Book ensureBookExists(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .customerName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

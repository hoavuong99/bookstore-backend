package com.example.bookstore.repository;

import com.example.bookstore.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}

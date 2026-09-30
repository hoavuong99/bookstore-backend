package com.example.bookstore.repository;

import com.example.bookstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

        Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);

    @Query("""
            select ci
            from CartItem ci
            join fetch ci.book b
            where ci.cart.id = :cartId
            """)
    List<CartItem> findItemsByCartId(@Param("cartId") Long cartId);

    @Modifying
    @Transactional
    @Query("""
            delete from CartItem ci
            where ci.cart.id = :cartId
            """)
    void deleteAllByCartId(@Param("cartId") Long cartId);
}
package com.example.bookstore.repository;

import com.example.bookstore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface OrderRepository extends JpaRepository<Order, Long> {
	Page<Order> findByUserId(Long userId, Pageable pageable);

	@Query("""
			select coalesce(sum(o.totalAmount), 0)
			from Order o
			where o.orderStatus = com.example.bookstore.enums.OrderStatus.DELIVERED
			""")
	BigDecimal sumCompletedRevenue();
}
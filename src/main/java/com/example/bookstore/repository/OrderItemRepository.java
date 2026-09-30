package com.example.bookstore.repository;

import com.example.bookstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	@Query("""
			select oi
			from OrderItem oi
			join fetch oi.book b
			where oi.order.id = :orderId
			""")
	List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}
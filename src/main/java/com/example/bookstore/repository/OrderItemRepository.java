package com.example.bookstore.repository;

import com.example.bookstore.entity.OrderItem;
import com.example.bookstore.dto.dashboard.BestSellerResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
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

	@Query("""
			select new com.example.bookstore.dto.dashboard.BestSellerResponse(
				oi.book.id,
				oi.book.title,
				sum(oi.quantity),
				oi.book.stockQuantity
			)
			from OrderItem oi
			where oi.order.orderStatus = com.example.bookstore.enums.OrderStatus.DELIVERED
			group by oi.book.id, oi.book.title, oi.book.stockQuantity
			order by sum(oi.quantity) desc
			""")
	List<BestSellerResponse> findBestSellers(Pageable pageable);
}
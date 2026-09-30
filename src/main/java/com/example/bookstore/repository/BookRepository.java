package com.example.bookstore.repository;

import com.example.bookstore.dto.dashboard.LowStockBookResponse;
import com.example.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query("""
	    select coalesce(sum(b.stockQuantity), 0)
	    from Book b
	    """)
    Long sumTotalStockQuantity();

    @Query("""
	    select count(b.id)
	    from Book b
	    """)
    Long countTotalBooks();

    @Query("""
	    select new com.example.bookstore.dto.dashboard.LowStockBookResponse(
		b.id,
		b.title,
		b.isbn,
		b.stockQuantity,
		b.price
	    )
	    from Book b
	    where b.stockQuantity < :threshold
	    order by b.stockQuantity asc, b.title asc
	    """)
    List<LowStockBookResponse> findLowStockBooks(@Param("threshold") Integer threshold);
}
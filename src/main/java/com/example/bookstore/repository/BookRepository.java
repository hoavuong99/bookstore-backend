package com.example.bookstore.repository;

import com.example.bookstore.dto.dashboard.LowStockBookResponse;
import com.example.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.math.BigDecimal;

public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
        select b
        from Book b
        where (:search is null or :search = '' or lower(b.title) like lower(concat('%', :search, '%')))
          and (:categoryId is null or exists (select category.id from Category category join category.books categorizedBook where categorizedBook.id = b.id and category.id = :categoryId))
          and (:maxPrice is null or b.price <= :maxPrice)
        """)
    Page<Book> searchBooks(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
    List<Book> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Book> findByEditorsPickTrueOrderByCreatedAtDesc(Pageable pageable);

	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, Long id);

	boolean existsByCategories_Id(Long categoryId);

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
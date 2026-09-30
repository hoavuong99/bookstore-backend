# GitHub Copilot System Instructions - Bookstore Backend

## 1. Project Overview & Architecture
- Domain: Online Bookstore Management System (RESTful API backend).
- Stack: Java 17/21, Spring Boot 3.x, Spring Data JPA (Hibernate), Spring Security 6 (Stateless JWT), MySQL 8.x (InnoDB), Lombok, Jakarta Validation.
- Architecture: Layered/Feature-driven (`controller` -> `service` -> `repository` -> `entity`).
- API Standards: Pure RESTful JSON. Always wrap controller payloads in `ApiResponse<T>`. Never expose JPA Entities directly via controllers; strictly map to/from Request/Response DTOs.

## 2. Database Schema Reference (12 Tables)
- Shared Auditing `BaseEntity`: `id` (BIGINT PK AUTO_INCREMENT), `created_at`, `updated_at`, `is_deleted` (BOOLEAN default false).
- Core Entities:
  - `users`: `email` (unique), `password`, `full_name`, `phone`, `address`, `role` (ADMIN, CUSTOMER, STAFF), `is_active`.
  - `categories`: `name` (unique), `description`.
  - `publishers`: `name` (unique), `address`, `phone`, `email`.
  - `authors`: `full_name`, `biography`.
  - `books`: `category_id` (FK), `publisher_id` (FK, nullable), `title`, `isbn` (unique), `price` (BigDecimal), `stock_quantity` (int), `published_year`, `page_count`, `image_url`, `description`.
  - `book_authors`: Composite PK (`book_id`, `author_id`) for N:N relation between Book and Author.
  - `carts`: `user_id` (FK, Unique - 1:1 relation).
  - `cart_items`: `cart_id` (FK), `book_id` (FK), `quantity` (int). Unique constraint on (`cart_id`, `book_id`).
  - `coupons`: `code` (unique), `discount_type` (PERCENTAGE, FIXED_AMOUNT), `discount_value` (BigDecimal), `min_order_amount`, `usage_limit`, `start_date`, `end_date`.
  - `orders`: `user_id` (FK), `coupon_id` (FK, nullable), `subtotal_amount`, `discount_amount`, `total_amount` (all BigDecimal), `shipping_address`, `recipient_name`, `recipient_phone`, `order_status` (PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED), `payment_method` (COD, VNPAY, MOMO), `payment_status` (UNPAID, PAID, FAILED, REFUNDED).
  - `order_items`: `order_id` (FK), `book_id` (FK), `quantity` (int), `price` (BigDecimal - snapshot price at order creation).
  - `reviews`: `user_id` (FK), `book_id` (FK), `rating` (1-5), `comment`. Unique constraint on (`user_id`, `book_id`).

## 3. Strict Coding Conventions & Rules
- Monetary Precision: Use `java.math.BigDecimal` exclusively for prices, subtotals, and discounts. Never use `double` or `float`.
- Relationship Fetching: All `@ManyToOne` and `@OneToMany` relationships must explicitly specify `fetch = FetchType.LAZY`.
- Dependency Injection: Use constructor injection via Lombok `@RequiredArgsConstructor`. Avoid `@Autowired` on fields.
- Validation: Use `@Valid` on `@RequestBody` parameters with Jakarta constraints (`@NotBlank`, `@NotNull`, `@Min`, `@Email`).
- Atomicity: The checkout process must be encapsulated in a single `@Transactional` method that validates coupons, verifies and decrements book stock, persists the order and snapshot item prices, and purges the user's cart.
- Error Handling: Handle exceptions globally using `@RestControllerAdvice`. Throw custom domain exceptions extending `RuntimeException` (e.g., `ResourceNotFoundException`, `BadRequestException`).
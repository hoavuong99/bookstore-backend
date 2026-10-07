package com.example.bookstore.service.impl;

import com.example.bookstore.exception.InsufficientStockException;
import com.example.bookstore.exception.InvalidCouponException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.entity.Coupon;
import com.example.bookstore.entity.Order;
import com.example.bookstore.entity.OrderItem;
import com.example.bookstore.entity.User;
import com.example.bookstore.enums.DiscountType;
import com.example.bookstore.enums.OrderStatus;
import com.example.bookstore.enums.PaymentStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartItemRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.CouponRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.dto.order.CheckoutRequest;
import com.example.bookstore.dto.order.CheckoutResponse;
import com.example.bookstore.dto.order.OrderDetailResponse;
import com.example.bookstore.dto.order.OrderItemResponse;
import com.example.bookstore.dto.order.OrderSummaryResponse;
import com.example.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        List<CartItem> cartItems = cartItemRepository.findItemsByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new InvalidCouponException("Cannot checkout with an empty cart");
        }

        BigDecimal subtotalAmount = calculateSubtotal(cartItems);

        Coupon coupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (StringUtils.hasText(request.getCouponCode())) {
                coupon = couponRepository.findByCode(request.getCouponCode().trim())
                    .orElseThrow(() -> new InvalidCouponException("Coupon not found or unavailable"));

            validateCoupon(coupon, subtotalAmount);
            discountAmount = calculateDiscount(subtotalAmount, coupon);
        }

        validateAndDeductStock(cartItems);

        BigDecimal totalAmount = subtotalAmount.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setUser(user);
        order.setCoupon(coupon);
        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setShippingAddress(request.getShippingAddress());
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(PaymentStatus.UNPAID);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setBook(cartItem.getBook());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getBook().getPrice());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);

        if (coupon != null && coupon.getUsageLimit() != null) {
            coupon.setUsageLimit(coupon.getUsageLimit() - 1);
            couponRepository.save(coupon);
        }

        cartItemRepository.deleteAllByCartId(cart.getId());

        return CheckoutResponse.builder()
                .orderId(savedOrder.getId())
                .subtotalAmount(subtotalAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .orderStatus(savedOrder.getOrderStatus())
                .paymentStatus(savedOrder.getPaymentStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(Long requesterId, Long orderId, boolean isAdminOrStaff) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!isAdminOrStaff && !order.getUser().getId().equals(requesterId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return mapToDetailResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(pageable)
                .map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setOrderStatus(status);
        Order savedOrder = orderRepository.save(order);
        return mapToDetailResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled.");
        }

        for (OrderItem orderItem : orderItemRepository.findByOrderId(order.getId())) {
            Book book = orderItem.getBook();
            book.setStockQuantity(book.getStockQuantity() + orderItem.getQuantity());
            bookRepository.save(book);
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }
        order.setPaymentUrl(null);
        order.setPaymentError("Order cancelled by customer.");
        return mapToDetailResponse(orderRepository.save(order));
    }

    private BigDecimal calculateSubtotal(List<CartItem> cartItems) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            BigDecimal lineTotal = cartItem.getBook().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }
        return subtotal;
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotalAmount) {
        LocalDate today = LocalDate.now();
        if (coupon.getStartDate() != null && today.isBefore(coupon.getStartDate())) {
            throw new InvalidCouponException("Coupon is not active yet");
        }

        if (coupon.getEndDate() != null && today.isAfter(coupon.getEndDate())) {
            throw new InvalidCouponException("Coupon has expired");
        }

        if (coupon.getMinOrderAmount() != null
                && subtotalAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new InvalidCouponException("Order does not meet coupon minimum amount");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageLimit() <= 0) {
            throw new InvalidCouponException("Coupon usage limit reached");
        }
    }

    private BigDecimal calculateDiscount(BigDecimal subtotalAmount, Coupon coupon) {
        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = subtotalAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(ONE_HUNDRED);
        } else {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(subtotalAmount) > 0) {
            return subtotalAmount;
        }
        return discount;
    }

    private void validateAndDeductStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            if (book.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for book id: " + book.getId()
                );
            }
        }

        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            int newStock = book.getStockQuantity() - cartItem.getQuantity();
            book.setStockQuantity(newStock);
            bookRepository.save(book);
        }
    }

        private OrderSummaryResponse mapToSummaryResponse(Order order) {
        return OrderSummaryResponse.builder()
            .orderId(order.getId())
            .userId(order.getUser().getId())
            .recipientName(order.getRecipientName())
            .recipientPhone(order.getRecipientPhone())
            .totalAmount(order.getTotalAmount())
            .orderStatus(order.getOrderStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .createdAt(order.getCreatedAt())
            .items(mapOrderItems(order))
            .build();
        }

        private List<OrderItemResponse> mapOrderItems(Order order) {
        return orderItemRepository.findByOrderId(order.getId()).stream()
            .map(orderItem -> OrderItemResponse.builder()
                .bookId(orderItem.getBook().getId())
                .bookTitle(orderItem.getBook().getTitle())
                .imageUrl(orderItem.getBook().getImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .build())
            .toList();
        }

        private OrderDetailResponse mapToDetailResponse(Order order) {
        return OrderDetailResponse.builder()
            .orderId(order.getId())
            .userId(order.getUser().getId())
            .shippingAddress(order.getShippingAddress())
            .recipientName(order.getRecipientName())
            .recipientPhone(order.getRecipientPhone())
            .subtotalAmount(order.getSubtotalAmount())
            .discountAmount(order.getDiscountAmount())
            .totalAmount(order.getTotalAmount())
            .orderStatus(order.getOrderStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .createdAt(order.getCreatedAt())
            .items(mapOrderItems(order))
            .build();
        }
}
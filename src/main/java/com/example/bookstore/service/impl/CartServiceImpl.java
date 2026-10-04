package com.example.bookstore.service.impl;

import com.example.bookstore.dto.cart.AddToCartRequest;
import com.example.bookstore.dto.cart.CartItemResponse;
import com.example.bookstore.dto.cart.CartResponse;
import com.example.bookstore.dto.cart.RemoveCartItemResponse;
import com.example.bookstore.entity.Book;
import com.example.bookstore.entity.Cart;
import com.example.bookstore.entity.CartItem;
import com.example.bookstore.entity.User;
import com.example.bookstore.exception.BadRequestException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CartItemRepository;
import com.example.bookstore.repository.CartRepository;
import com.example.bookstore.repository.UserRepository;
import com.example.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public CartItemResponse addBookToCart(Long userId, AddToCartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.getBookId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        CartItem cartItem = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setBook(book);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int updatedQuantity = cartItem.getQuantity() + request.getQuantity();
        if (updatedQuantity > book.getStockQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock for book id: " + book.getId());
        }

        cartItem.setQuantity(updatedQuantity);
        CartItem savedItem = cartItemRepository.save(cartItem);

        return CartItemResponse.builder()
                .itemId(savedItem.getId())
                .cartId(cart.getId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .imageUrl(book.getImageUrl())
                .unitPrice(book.getPrice())
                .quantity(savedItem.getQuantity())
                .lineTotal(book.getPrice().multiply(BigDecimal.valueOf(savedItem.getQuantity())))
                .build();
    }

            @Override
            @Transactional(readOnly = true)
            public CartResponse getCart(Long userId) {
            Cart cart = cartRepository.findByUserId(userId).orElse(null);
            if (cart == null) {
                return CartResponse.builder()
                    .totalItems(0)
                    .subtotal(BigDecimal.ZERO)
                    .items(List.of())
                    .build();
            }

            List<CartItemResponse> items = cartItemRepository.findItemsByCartId(cart.getId()).stream()
                .map(cartItem -> CartItemResponse.builder()
                    .itemId(cartItem.getId())
                    .cartId(cart.getId())
                    .bookId(cartItem.getBook().getId())
                    .bookTitle(cartItem.getBook().getTitle())
                    .imageUrl(cartItem.getBook().getImageUrl())
                    .unitPrice(cartItem.getBook().getPrice())
                    .quantity(cartItem.getQuantity())
                    .lineTotal(cartItem.getBook().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build())
                .toList();

            int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
            BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return CartResponse.builder()
                .cartId(cart.getId())
                .totalItems(totalItems)
                .subtotal(subtotal)
                .items(items)
                .build();
            }

            @Override
            @Transactional
            public RemoveCartItemResponse removeCartItem(Long userId, Long itemId) {
            CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

            Long ownerUserId = cartItem.getCart().getUser().getId();
            if (!ownerUserId.equals(userId)) {
                throw new BadRequestException("Cart item does not belong to current user");
            }

            Long cartId = cartItem.getCart().getId();
            cartItemRepository.delete(cartItem);

            int remainingItems = cartItemRepository.findItemsByCartId(cartId).size();

            return RemoveCartItemResponse.builder()
                .cartId(cartId)
                .itemId(itemId)
                .remainingItems(remainingItems)
                .build();
            }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cartItemRepository.deleteAllByCartId(cart.getId());
        }

        return CartResponse.builder()
                .cartId(cart == null ? null : cart.getId())
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .items(List.of())
                .build();
    }
}

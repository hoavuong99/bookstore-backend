package com.example.bookstore.service.impl;

import com.example.bookstore.dto.dashboard.DashboardSummaryResponse;
import com.example.bookstore.dto.dashboard.LowStockBookResponse;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    @Value("${app.dashboard.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        BigDecimal totalRevenue = orderRepository.sumCompletedRevenue();
        Long totalStockQuantity = bookRepository.sumTotalStockQuantity();
        Long totalBooks = bookRepository.countTotalBooks();
        List<LowStockBookResponse> lowStockBooks = bookRepository.findLowStockBooks(lowStockThreshold);

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalStockQuantity(totalStockQuantity != null ? totalStockQuantity : 0L)
                .totalBooks(totalBooks != null ? totalBooks : 0L)
                .lowStockBooks(lowStockBooks)
                .build();
    }
}

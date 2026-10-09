package com.example.bookstore.service.impl;

import com.example.bookstore.dto.dashboard.DashboardSummaryResponse;
import com.example.bookstore.dto.dashboard.BestSellerResponse;
import com.example.bookstore.dto.dashboard.LowStockBookResponse;
import com.example.bookstore.dto.dashboard.RevenuePointResponse;
import com.example.bookstore.enums.OrderStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.OrderRepository;
import com.example.bookstore.repository.OrderItemRepository;
import com.example.bookstore.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.dashboard.low-stock-threshold:10}")
    private int lowStockThreshold;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary(String period, LocalDate from, LocalDate to) {
        if (!List.of("day", "month", "year").contains(period)) {
            throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        }
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = period.equals("day")
                ? defaultTo.minusDays(2)
                : period.equals("month")
                    ? defaultTo.withDayOfMonth(1).minusMonths(2)
                    : defaultTo.withDayOfYear(1).minusYears(2);
        LocalDate selectedFrom = from != null ? from : defaultFrom;
        LocalDate selectedTo = to != null ? to : defaultTo;
        if (selectedFrom.isAfter(selectedTo)) {
            throw new IllegalArgumentException("Khoảng thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        LocalDateTime start = getRangeStart(period, selectedFrom);
        LocalDateTime end = getRangeEnd(period, selectedTo);
        List<com.example.bookstore.entity.Order> completedOrders =
                orderRepository.findByOrderStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        OrderStatus.DELIVERED, start, end);
        BigDecimal totalRevenue = completedOrders.stream()
                .map(com.example.bookstore.entity.Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Long totalStockQuantity = bookRepository.sumTotalStockQuantity();
        Long totalBooks = bookRepository.countTotalBooks();
        List<LowStockBookResponse> lowStockBooks = bookRepository.findLowStockBooks(lowStockThreshold);
        List<BestSellerResponse> bestSellers =
                orderItemRepository.findBestSellers(PageRequest.of(0, 3));

        return DashboardSummaryResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalStockQuantity(totalStockQuantity != null ? totalStockQuantity : 0L)
                .totalBooks(totalBooks != null ? totalBooks : 0L)
                .lowStockBooks(lowStockBooks)
                .bestSellers(bestSellers)
                .revenuePoints(buildRevenuePoints(period, selectedFrom, selectedTo, completedOrders))
                .build();
    }

    private LocalDateTime getRangeStart(String period, LocalDate date) {
        return switch (period) {
            case "day" -> date.atStartOfDay();
            case "month" -> date.withDayOfMonth(1).atStartOfDay();
            case "year" -> date.withDayOfYear(1).atStartOfDay();
            default -> throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        };
    }

    private LocalDateTime getRangeEnd(String period, LocalDate date) {
        return switch (period) {
            case "day" -> date.plusDays(1).atStartOfDay();
            case "month" -> YearMonth.from(date).plusMonths(1).atDay(1).atStartOfDay();
            case "year" -> date.plusYears(1).withDayOfYear(1).atStartOfDay();
            default -> throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        };
    }

    private List<RevenuePointResponse> buildRevenuePoints(
            String period,
            LocalDate from,
            LocalDate to,
            List<com.example.bookstore.entity.Order> orders
    ) {
        List<RevenuePointResponse> points = new ArrayList<>();
        LocalDate cursor = period.equals("month") ? from.withDayOfMonth(1)
                : period.equals("year") ? from.withDayOfYear(1) : from;
        LocalDate last = period.equals("month") ? to.withDayOfMonth(1)
                : period.equals("year") ? to.withDayOfYear(1) : to;
        while (!cursor.isAfter(last)) {
            LocalDateTime pointStart = getRangeStart(period, cursor);
            LocalDateTime pointEnd = getRangeEnd(period, cursor);
            BigDecimal revenue = orders.stream()
                    .filter(order -> !order.getCreatedAt().isBefore(pointStart)
                            && order.getCreatedAt().isBefore(pointEnd))
                    .map(com.example.bookstore.entity.Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String label = period.equals("day") ? cursor.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
                    : period.equals("month") ? cursor.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))
                    : String.valueOf(cursor.getYear());
            points.add(RevenuePointResponse.builder().label(label).revenue(revenue).build());
            cursor = period.equals("day") ? cursor.plusDays(1)
                    : period.equals("month") ? cursor.plusMonths(1) : cursor.plusYears(1);
        }
        return points;
    }
}

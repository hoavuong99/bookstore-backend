package com.example.bookstore.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {
    private BigDecimal totalRevenue;
    private Long totalStockQuantity;
    private Long totalBooks;
    private List<LowStockBookResponse> lowStockBooks;
}

package com.example.bookstore.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RevenuePointResponse {
    private String label;
    private BigDecimal revenue;
}

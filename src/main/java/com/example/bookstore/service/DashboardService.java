package com.example.bookstore.service;

import com.example.bookstore.dto.dashboard.DashboardSummaryResponse;
import java.time.LocalDate;

public interface DashboardService {
    DashboardSummaryResponse getDashboardSummary(String period, LocalDate from, LocalDate to);
}

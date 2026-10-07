package com.example.bookstore.controller;

import com.example.bookstore.common.ApiResponse;
import com.example.bookstore.service.ZaloPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ZaloPayService zaloPayService;

    @PostMapping("/zalopay/callback")
    public ResponseEntity<Map<String, Object>> handleZaloPayCallback(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(zaloPayService.callback(body.get("data"), body.get("mac")));
    }
}

package com.example.bookstore.service;

import com.example.bookstore.dto.order.PaymentResponse;
import com.example.bookstore.entity.Order;
import com.example.bookstore.enums.PaymentMethod;
import com.example.bookstore.enums.PaymentStatus;
import com.example.bookstore.enums.OrderStatus;
import com.example.bookstore.exception.BusinessException;
import com.example.bookstore.exception.ResourceNotFoundException;
import com.example.bookstore.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZaloPayService {

    private static final String PROVIDER = "zalopay";
    private static final DateTimeFormatter TRANSACTION_DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.zalopay.endpoint}")
    private String endpoint;

    @Value("${app.payment.zalopay.query-endpoint}")
    private String queryEndpoint;

    @Value("${app.payment.zalopay.app-id}")
    private String appId;

    @Value("${app.payment.zalopay.key1}")
    private String key1;

    @Value("${app.payment.zalopay.key2}")
    private String key2;

    @Value("${app.payment.zalopay.callback-url}")
    private String callbackUrl;

    @Value("${app.payment.zalopay.redirect-url}")
    private String redirectUrl;

    @Value("${app.payment.zalopay.session-minutes:15}")
    private long sessionMinutes;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional
    public PaymentResponse createSession(Long userId, Long orderId) {
        Order order = ownedOrder(userId, orderId);
        ensureConfigured();
        ensureZaloPayOrder(order);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return toResponse(order);
        }
        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new BusinessException("This payment has failed and cannot be restarted.");
        }
        if (order.getPaymentTransactionId() != null && order.getPaymentUrl() != null
                && order.getPaymentExpiresAt() != null && order.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
            return toResponse(order);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setPaymentTransactionId(transactionId(order.getId()));
        order.setPaymentStartedAt(now);
        order.setPaymentExpiresAt(now.plusMinutes(sessionMinutes));
        order.setPaymentSessionState("CREATING");
        order.setPaymentError(null);
        order = orderRepository.save(order);

        try {
            JsonNode response = postCreate(order);
            if (response.path("return_code").asInt() != 1 || response.path("order_url").asText().isBlank()) {
                throw new BusinessException(response.path("return_message").asText("ZaloPay could not create a payment session."));
            }
            order.setPaymentUrl(response.path("order_url").asText());
            order.setPaymentSessionState("READY");
            return toResponse(orderRepository.save(order));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            markCreationFailure(order, "ZaloPay request was interrupted.");
            throw new BusinessException("Unable to start ZaloPay payment.");
        } catch (Exception exception) {
            markCreationFailure(order, exception.getMessage());
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("Unable to start ZaloPay payment.");
        }
    }

    @Transactional
    public PaymentResponse refresh(Long userId, Long orderId) {
        Order order = ownedOrder(userId, orderId);
        if (!PROVIDER.equals(order.getPaymentProvider()) || order.getPaymentTransactionId() == null) {
            return toResponse(order);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return toResponse(order);
        }

        try {
            JsonNode response = postQuery(order.getPaymentTransactionId());
            int returnCode = response.path("return_code").asInt();
            if (returnCode == 1) {
                markPaid(order, response.path("zp_trans_id").asText());
            } else if (returnCode == 2) {
                cancelUnpaidOrder(order, "ZaloPay reported that the payment failed.");
            }
            return toResponse(orderRepository.save(order));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Unable to verify ZaloPay payment.");
        } catch (Exception exception) {
            throw new BusinessException("Unable to verify ZaloPay payment.");
        }
    }

    @Transactional
    public Map<String, Object> callback(String data, String mac) {
        ensureConfigured();
        final String expectedMac;
        try {
            expectedMac = sign(key2, data);
        } catch (Exception exception) {
            throw new BusinessException("Unable to verify ZaloPay callback signature.");
        }
        if (!secureEquals(expectedMac, mac)) {
            throw new BusinessException("Invalid ZaloPay callback signature.");
        }
        try {
            JsonNode callbackData = objectMapper.readTree(data);
            String transactionId = callbackData.path("app_trans_id").asText();
            Order order = orderRepository.findByPaymentTransactionId(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found."));
            if (callbackData.has("status") && callbackData.path("status").asInt() != 1) {
                cancelUnpaidOrder(order, "ZaloPay reported that the payment was not completed.");
                orderRepository.save(order);
                return Map.of("return_code", 1, "return_message", "Payment failure processed");
            }
            long amount = callbackData.path("amount").asLong();
            if (amount != gatewayAmount(order)) {
                throw new BusinessException("ZaloPay payment amount does not match the order.");
            }
            markPaid(order, callbackData.path("zp_trans_id").asText());
            orderRepository.save(order);
            return Map.of("return_code", 1, "return_message", "Success");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("Invalid ZaloPay callback.");
        }
    }

    private JsonNode postCreate(Order order) throws Exception {
        long amount = gatewayAmount(order);
        String item = objectMapper.writeValueAsString(order.getOrderItems().stream()
                .map(itemEntry -> Map.of("itemid", itemEntry.getBook().getId(), "itemname", itemEntry.getBook().getTitle(),
                        "itemprice", itemEntry.getPrice().setScale(0),
                        "itemquantity", itemEntry.getQuantity()))
                .toList());
        String embedData = objectMapper.writeValueAsString(Map.of("redirecturl", redirectUrl));
        long appTime = System.currentTimeMillis();
        String mac = sign(key1, String.join("|", appId, order.getPaymentTransactionId(), "bookstore-user",
                String.valueOf(amount), String.valueOf(appTime), embedData, item));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("app_id", Integer.parseInt(appId));
        payload.put("app_user", "bookstore-user");
        payload.put("app_trans_id", order.getPaymentTransactionId());
        payload.put("app_time", appTime);
        payload.put("amount", amount);
        payload.put("description", "Bookstore order #" + order.getId());
        payload.put("bank_code", "");
        payload.put("item", item);
        payload.put("embed_data", embedData);
        payload.put("callback_url", callbackUrl);
        payload.put("mac", mac);
        return post(endpoint, payload);
    }

    private JsonNode postQuery(String transactionId) throws Exception {
        String mac = sign(key1, appId + "|" + transactionId);
        return post(queryEndpoint, Map.of("app_id", Integer.parseInt(appId), "app_trans_id", transactionId, "mac", mac));
    }

    private JsonNode post(String url, Map<String, Object> payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new BusinessException("ZaloPay returned HTTP " + response.statusCode() + ".");
        }
        return objectMapper.readTree(response.body());
    }

    private void markPaid(Order order, String gatewayTransactionId) {
        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        }
        order.setPaymentSessionState("PAID");
        order.setPaymentGatewayTransactionId(gatewayTransactionId);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentError(null);
    }

    private void cancelUnpaidOrder(Order order, String reason) {
        if (order.getPaymentStatus() != PaymentStatus.UNPAID || order.getOrderStatus() != OrderStatus.PENDING) {
            return;
        }
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentSessionState("FAILED");
        order.setPaymentUrl(null);
        order.setPaymentError(reason);
        for (var orderItem : order.getOrderItems()) {
            var book = orderItem.getBook();
            book.setStockQuantity(book.getStockQuantity() + orderItem.getQuantity());
        }
    }

    private void markCreationFailure(Order order, String message) {
        order.setPaymentSessionState("FAILED");
        order.setPaymentError(message == null || message.isBlank() ? "ZaloPay session creation failed." : message);
        orderRepository.save(order);
    }

    private Order ownedOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found.");
        }
        return order;
    }

    private void ensureZaloPayOrder(Order order) {
        if (order.getPaymentMethod() != PaymentMethod.ZALOPAY) {
            throw new BusinessException("This order does not use ZaloPay.");
        }
        order.setPaymentProvider(PROVIDER);
    }

    private PaymentResponse toResponse(Order order) {
        return PaymentResponse.builder()
                .orderId(order.getId())
                .provider(order.getPaymentProvider())
                .paymentStatus(order.getPaymentStatus())
                .state(order.getPaymentSessionState())
                .paymentUrl(order.getPaymentUrl())
                .expiresAt(order.getPaymentExpiresAt())
                .error(order.getPaymentError())
                .build();
    }

    private String transactionId(Long orderId) {
        return LocalDate.now().format(TRANSACTION_DATE) + "_" + orderId;
    }

    private long gatewayAmount(Order order) {
        return order.getTotalAmount().setScale(0).longValueExact();
    }

    private void ensureConfigured() {
        if (appId.isBlank() || key1.isBlank() || key2.isBlank()) {
            throw new BusinessException("ZaloPay sandbox is not configured.");
        }
    }

    private String sign(String key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean secureEquals(String expected, String actual) {
        return MacUtils.constantTimeEquals(expected, actual);
    }

    private static final class MacUtils {
        private static boolean constantTimeEquals(String expected, String actual) {
            if (expected == null || actual == null) return false;
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
        }
    }
}

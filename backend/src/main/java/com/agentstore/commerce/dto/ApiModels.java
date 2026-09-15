package com.agentstore.commerce.dto;

import com.agentstore.commerce.domain.AfterSaleStatus;
import com.agentstore.commerce.domain.AfterSaleType;
import com.agentstore.commerce.domain.BalanceRecordType;
import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.domain.UserRole;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiModels {

    private ApiModels() {
    }

    public record ProductResponse(
        Long id,
        String sku,
        String name,
        String category,
        String description,
        BigDecimal price,
        BigDecimal promotionPrice,
        BigDecimal salePrice,
        Integer stock,
        String imageUrl,
        Boolean active
    ) {
    }

    public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "用户名需为 4-30 位字母、数字或下划线") String username,
        @NotBlank @Size(min = 6, max = 50) String password,
        @NotBlank @Size(max = 50) String displayName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$", message = "手机号格式不正确") String phone
    ) {
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {
    }

    public record UserResponse(
        Long id,
        String username,
        String displayName,
        String phone,
        String address,
        UserRole role,
        BigDecimal balance,
        LocalDateTime createdAt
    ) {
    }

    public record ProfileUpdateRequest(
        @NotBlank @Size(max = 50) String displayName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$", message = "手机号格式不正确") String phone,
        @Size(max = 300) String address
    ) {
    }

    public record BalanceRecordResponse(
        Long id,
        BalanceRecordType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        LocalDateTime createdAt
    ) {
    }

    public record CartItemCreateRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
    ) {
    }

    public record CartItemUpdateRequest(
        @NotNull @Min(1) Integer quantity
    ) {
    }

    public record CartItemResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Integer stock
    ) {
    }

    public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        Integer itemCount,
        BigDecimal totalAmount
    ) {
    }

    public record CreateOrderRequest(
        @NotBlank @Size(max = 50) String receiverName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$", message = "收货电话格式不正确") String receiverPhone,
        @NotBlank @Size(max = 300) String shippingAddress
    ) {
    }

    public record OrderItemResponse(
        Long productId,
        String sku,
        String productName,
        String imageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
    ) {
    }

    public record OrderResponse(
        Long id,
        String orderNo,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        String trackingNo,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
    ) {
    }

    public record CreateAfterSaleRequest(
        @NotBlank String orderNo,
        @NotNull AfterSaleType type,
        @NotBlank @Size(max = 500) String reason
    ) {
    }

    public record AfterSaleResponse(
        Long id,
        String afterSaleNo,
        String orderNo,
        Long userId,
        AfterSaleType type,
        AfterSaleStatus status,
        String reason,
        String adminRemark,
        String returnCarrier,
        String returnTrackingNo,
        BigDecimal refundAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    public record ProductSaveRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 1000) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.01") BigDecimal promotionPrice,
        @NotNull @Min(0) Integer stock,
        @NotBlank @Size(max = 500) String imageUrl,
        @NotNull Boolean active
    ) {
    }

    public record ShipOrderRequest(
        @NotBlank @Size(max = 80) String trackingNo
    ) {
    }

    public record ReviewAfterSaleRequest(
        @NotNull Boolean approved,
        @NotBlank @Size(max = 500) String remark
    ) {
    }

    public record ReturnShipmentRequest(
        @NotBlank @Size(max = 50) String carrier,
        @NotBlank @Size(max = 80) String trackingNo
    ) {
    }

    public record ConfirmReceiptRequest(
        @NotBlank @Size(max = 500) String remark
    ) {
    }

    public record BalanceAdjustRequest(
        @NotNull BigDecimal amount,
        @NotBlank @Size(max = 200) String description
    ) {
    }
}

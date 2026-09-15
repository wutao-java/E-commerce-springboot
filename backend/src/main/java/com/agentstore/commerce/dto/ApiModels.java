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
        Boolean active,
        String highlights,
        Boolean supportsSevenDayReturn,
        String afterSaleNote,
        String scenarioTags,
        PromotionResponse promotion,
        Boolean promotionApplied,
        String promotionCondition
    ) {
    }

    public record PromotionResponse(
        Long id,
        Long productId,
        String promotionName,
        String promotionType,
        String discountSummary,
        BigDecimal promotionPrice,
        String requiredMemberLevel,
        String conditionSummary,
        LocalDateTime startAt,
        LocalDateTime endAt,
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
        LocalDateTime createdAt,
        String businessUserId,
        String memberLevel,
        String riskLevel,
        String preferredCategories,
        String preferredDelivery,
        BigDecimal budgetMin,
        BigDecimal budgetMax,
        Boolean invoiceRequired
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
        @NotNull @Min(1) Integer quantity,
        Boolean selected
    ) {
        public CartItemCreateRequest(Long productId, Integer quantity) {
            this(productId, quantity, true);
        }
    }

    public record CartItemUpdateRequest(
        @Min(1) Integer quantity,
        Boolean selected
    ) {
        public CartItemUpdateRequest(Integer quantity) {
            this(quantity, null);
        }
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
        Integer stock,
        Boolean selected,
        Boolean settlementAvailable,
        String unavailableReason,
        String promotionName,
        String promotionCondition
    ) {
    }

    public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        Integer itemCount,
        BigDecimal totalAmount,
        Integer selectedItemCount,
        BigDecimal selectedTotalAmount
    ) {
    }

    public record CreateOrderRequest(
        @NotBlank @Size(max = 50) String receiverName,
        @NotBlank @Pattern(regexp = "^[0-9+ -]{6,30}$", message = "收货电话格式不正确") String receiverPhone,
        @NotBlank @Size(max = 300) String shippingAddress,
        String source,
        List<Long> cartItemIds,
        Long productId,
        @Min(1) Integer quantity,
        @Size(max = 500) String remark
    ) {
        public CreateOrderRequest(String receiverName, String receiverPhone, String shippingAddress) {
            this(receiverName, receiverPhone, shippingAddress, "CART", null, null, null, "");
        }
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
        List<OrderItemResponse> items,
        String paymentStatus,
        String fulfillmentStatus,
        String remark,
        List<LogisticsEventResponse> logisticsEvents,
        Boolean afterSaleAvailable,
        List<AfterSaleType> availableAfterSaleTypes
    ) {
    }

    public record LogisticsEventResponse(
        Long id,
        String carrier,
        String trackingNo,
        String status,
        String content,
        LocalDateTime occurredAt
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
        BigDecimal approvedAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApprovalRecordResponse> approvalRecords
    ) {
    }

    public record ApprovalRecordResponse(
        Long id,
        String action,
        String remark,
        BigDecimal approvedAmount,
        LocalDateTime createdAt
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
        @NotNull Boolean active,
        @Size(max = 500) String highlights,
        Boolean supportsSevenDayReturn,
        @Size(max = 500) String afterSaleNote,
        @Size(max = 300) String scenarioTags
    ) {
        public ProductSaveRequest(String sku, String name, String category, String description,
                                  BigDecimal price, BigDecimal promotionPrice, Integer stock,
                                  String imageUrl, Boolean active) {
            this(sku, name, category, description, price, promotionPrice, stock, imageUrl, active,
                "", true, "", "");
        }
    }

    public record ShipOrderRequest(
        @Size(max = 50) String carrier,
        @NotBlank @Size(max = 80) String trackingNo
    ) {
        public ShipOrderRequest(String trackingNo) {
            this("顺丰速运", trackingNo);
        }
    }

    public record ReviewAfterSaleRequest(
        @NotNull Boolean approved,
        @NotBlank @Size(max = 500) String remark,
        @DecimalMin(value = "0.01") BigDecimal approvedAmount
    ) {
        public ReviewAfterSaleRequest(Boolean approved, String remark) {
            this(approved, remark, null);
        }
    }

    public record SupplementAfterSaleRequest(
        @NotBlank @Size(max = 500) String content
    ) {
    }

    public record NeedMoreInfoRequest(
        @NotBlank @Size(max = 500) String remark
    ) {
    }

    public record LogisticsEventRequest(
        @NotBlank @Size(max = 50) String carrier,
        @NotBlank @Size(max = 80) String trackingNo,
        @NotBlank @Size(max = 30) String status,
        @NotBlank @Size(max = 300) String content
    ) {
    }

    public record PromotionSaveRequest(
        @NotNull Long productId,
        @NotBlank @Size(max = 100) String promotionName,
        @NotBlank @Size(max = 40) String promotionType,
        @NotBlank @Size(max = 300) String discountSummary,
        @NotNull @DecimalMin(value = "0.01") BigDecimal promotionPrice,
        @Size(max = 20) String requiredMemberLevel,
        @Size(max = 300) String conditionSummary,
        LocalDateTime startAt,
        LocalDateTime endAt,
        @NotNull Boolean active
    ) {
    }

    public record AfterSalePolicyResponse(
        Long id,
        String sceneKey,
        String title,
        String content,
        String applicableConditions,
        String exclusionConditions,
        String requiredEvidence,
        Boolean requiresManualReview
    ) {
    }

    public record FaqResponse(Long id, String category, String question, String answer) {
    }

    public record CommerceProfileRequest(
        @Size(max = 300) String preferredCategories,
        @Size(max = 100) String preferredDelivery,
        @DecimalMin(value = "0.00") BigDecimal budgetMin,
        @DecimalMin(value = "0.00") BigDecimal budgetMax,
        Boolean invoiceRequired
    ) {
    }

    public record CustomerServiceRequest(
        @NotBlank @Size(max = 2000) String message,
        String sessionId,
        java.util.Map<String, Object> pageContext
    ) {
    }

    public record CustomerServiceResponse(
        String answer,
        String sessionId,
        Boolean fallback,
        java.util.Map<String, Object> sessionState
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

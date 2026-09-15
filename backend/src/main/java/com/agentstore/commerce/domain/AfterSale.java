package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "after_sales")
public class AfterSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "after_sale_no", nullable = false, unique = true, length = 40)
    private String afterSaleNo;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_sale_type", nullable = false, length = 20)
    private AfterSaleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AfterSaleStatus status;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "admin_remark", length = 500)
    private String adminRemark;

    @Column(name = "return_carrier", length = 50)
    private String returnCarrier;

    @Column(name = "return_tracking_no", length = 80)
    private String returnTrackingNo;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AfterSale() {
    }

    public AfterSale(String afterSaleNo, Long orderId, String orderNo, Long userId, AfterSaleType type,
                     String reason, BigDecimal refundAmount, LocalDateTime now) {
        this.afterSaleNo = afterSaleNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.type = type;
        this.reason = reason;
        this.refundAmount = refundAmount;
        this.status = AfterSaleStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void approveReturn(String remark, LocalDateTime now) {
        this.status = AfterSaleStatus.WAITING_RETURN;
        this.adminRemark = remark;
        this.updatedAt = now;
    }

    public void submitReturn(String carrier, String trackingNo, LocalDateTime now) {
        this.status = AfterSaleStatus.WAITING_RECEIPT;
        this.returnCarrier = carrier;
        this.returnTrackingNo = trackingNo;
        this.updatedAt = now;
    }

    public void completeRefund(String remark, LocalDateTime now) {
        this.status = AfterSaleStatus.APPROVED;
        this.adminRemark = remark;
        this.updatedAt = now;
    }

    public void reject(String remark, LocalDateTime now) {
        this.status = AfterSaleStatus.REJECTED;
        this.adminRemark = remark;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getAfterSaleNo() {
        return afterSaleNo;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public AfterSaleType getType() {
        return type;
    }

    public AfterSaleStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getAdminRemark() {
        return adminRemark;
    }

    public String getReturnCarrier() {
        return returnCarrier;
    }

    public String getReturnTrackingNo() {
        return returnTrackingNo;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

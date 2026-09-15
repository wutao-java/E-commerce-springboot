package com.agentstore.commerce.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "shipping_address", nullable = false, length = 300)
    private String shippingAddress;

    @Column(name = "tracking_no", length = 80)
    private String trackingNo;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(String orderNo, Long userId, BigDecimal totalAmount, String receiverName,
                         String receiverPhone, String shippingAddress, LocalDateTime now) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalAmount = totalAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.shippingAddress = shippingAddress;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void cancel(LocalDateTime now) {
        this.status = OrderStatus.CANCELED;
        this.updatedAt = now;
    }

    public void pay(LocalDateTime now) {
        this.status = OrderStatus.PAID;
        this.paidAt = now;
        this.updatedAt = now;
    }

    public void ship(String trackingNo, LocalDateTime now) {
        this.status = OrderStatus.SHIPPED;
        this.trackingNo = trackingNo;
        this.shippedAt = now;
        this.updatedAt = now;
    }

    public void complete(LocalDateTime now) {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void startAfterSale(LocalDateTime now) {
        this.status = OrderStatus.AFTER_SALE;
        this.updatedAt = now;
    }

    public void rejectAfterSale(LocalDateTime now) {
        if (completedAt != null) {
            this.status = OrderStatus.COMPLETED;
        } else if (shippedAt != null) {
            this.status = OrderStatus.SHIPPED;
        } else {
            this.status = OrderStatus.PAID;
        }
        this.updatedAt = now;
    }

    public void refund(LocalDateTime now) {
        this.status = OrderStatus.REFUNDED;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}

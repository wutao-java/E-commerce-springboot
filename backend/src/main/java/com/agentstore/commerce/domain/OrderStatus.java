package com.agentstore.commerce.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    AFTER_SALE,
    REFUNDED,
    CANCELED
}

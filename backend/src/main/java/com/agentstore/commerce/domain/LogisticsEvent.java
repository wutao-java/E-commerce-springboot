package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_events")
public class LogisticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 50)
    private String carrier;

    @Column(name = "tracking_no", nullable = false, length = 80)
    private String trackingNo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    protected LogisticsEvent() {
    }

    public LogisticsEvent(Long orderId, String carrier, String trackingNo, String status,
                          String content, LocalDateTime occurredAt) {
        this.orderId = orderId;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.status = status;
        this.content = content;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public String getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

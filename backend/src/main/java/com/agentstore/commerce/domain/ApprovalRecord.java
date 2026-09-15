package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_records")
public class ApprovalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "after_sale_id", nullable = false)
    private Long afterSaleId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 500)
    private String remark;

    @Column(name = "approved_amount", precision = 12, scale = 2)
    private BigDecimal approvedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ApprovalRecord() {
    }

    public ApprovalRecord(Long afterSaleId, String action, String remark,
                          BigDecimal approvedAmount, LocalDateTime createdAt) {
        this.afterSaleId = afterSaleId;
        this.action = action;
        this.remark = remark;
        this.approvedAmount = approvedAmount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAfterSaleId() {
        return afterSaleId;
    }

    public String getAction() {
        return action;
    }

    public String getRemark() {
        return remark;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

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
@Table(name = "product_promotions")
public class ProductPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "promotion_name", nullable = false, length = 100)
    private String promotionName;

    @Column(name = "promotion_type", nullable = false, length = 40)
    private String promotionType;

    @Column(name = "discount_summary", nullable = false, length = 300)
    private String discountSummary;

    @Column(name = "promotion_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal promotionPrice;

    @Column(name = "required_member_level", length = 20)
    private String requiredMemberLevel;

    @Column(name = "condition_summary", length = 300)
    private String conditionSummary;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    private Boolean active;

    protected ProductPromotion() {
    }

    public ProductPromotion(Long productId, String promotionName, String promotionType,
                            String discountSummary, BigDecimal promotionPrice,
                            String requiredMemberLevel, String conditionSummary,
                            LocalDateTime startAt, LocalDateTime endAt, Boolean active) {
        this.productId = productId;
        update(promotionName, promotionType, discountSummary, promotionPrice, requiredMemberLevel,
            conditionSummary, startAt, endAt, active);
    }

    public void update(String promotionName, String promotionType, String discountSummary,
                       BigDecimal promotionPrice, String requiredMemberLevel, String conditionSummary,
                       LocalDateTime startAt, LocalDateTime endAt, Boolean active) {
        this.promotionName = promotionName;
        this.promotionType = promotionType;
        this.discountSummary = discountSummary;
        this.promotionPrice = promotionPrice;
        this.requiredMemberLevel = requiredMemberLevel;
        this.conditionSummary = conditionSummary;
        this.startAt = startAt;
        this.endAt = endAt;
        this.active = active;
    }

    public boolean isEffective(LocalDateTime now) {
        return Boolean.TRUE.equals(active)
            && (startAt == null || !startAt.isAfter(now))
            && (endAt == null || !endAt.isBefore(now));
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public String getPromotionType() {
        return promotionType;
    }

    public String getDiscountSummary() {
        return discountSummary;
    }

    public BigDecimal getPromotionPrice() {
        return promotionPrice;
    }

    public String getRequiredMemberLevel() {
        return requiredMemberLevel;
    }

    public String getConditionSummary() {
        return conditionSummary;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public Boolean getActive() {
        return active;
    }
}

package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(name = "uk_cart_user_product", columnNames = {"user_id", "product_id"})
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Boolean selected = true;

    protected CartItem() {
    }

    public CartItem(Long userId, Long productId, Integer quantity) {
        this(userId, productId, quantity, true);
    }

    public CartItem(Long userId, Long productId, Integer quantity, Boolean selected) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.selected = selected == null || selected;
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void update(Integer quantity, Boolean selected) {
        if (quantity != null) {
            this.quantity = quantity;
        }
        if (selected != null) {
            this.selected = selected;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Boolean getSelected() {
        return selected == null || selected;
    }
}

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
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(length = 30)
    private String phone;

    @Column(length = 300)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "business_user_id", unique = true, length = 30)
    private String businessUserId;

    @Column(name = "member_level", nullable = false, length = 20)
    private String memberLevel = "normal";

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "low";

    @Column(name = "preferred_categories", nullable = false, length = 300)
    private String preferredCategories = "";

    @Column(name = "preferred_delivery", nullable = false, length = 100)
    private String preferredDelivery = "";

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "invoice_required", nullable = false)
    private Boolean invoiceRequired = false;

    protected UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String displayName, String phone,
                       String address, UserRole role, BigDecimal balance, LocalDateTime createdAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.balance = balance;
        this.createdAt = createdAt;
        this.memberLevel = "normal";
        this.riskLevel = "low";
        this.preferredCategories = "";
        this.preferredDelivery = "";
        this.invoiceRequired = false;
    }

    public void updateProfile(String displayName, String phone, String address) {
        this.displayName = displayName;
        this.phone = phone;
        this.address = address;
    }

    public void changeBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void updateCommerceProfile(String businessUserId, String memberLevel, String riskLevel,
                                      String preferredCategories, String preferredDelivery,
                                      BigDecimal budgetMin, BigDecimal budgetMax, Boolean invoiceRequired) {
        this.businessUserId = businessUserId;
        this.memberLevel = memberLevel == null ? "normal" : memberLevel;
        this.riskLevel = riskLevel == null ? "low" : riskLevel;
        this.preferredCategories = preferredCategories == null ? "" : preferredCategories;
        this.preferredDelivery = preferredDelivery == null ? "" : preferredDelivery;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.invoiceRequired = Boolean.TRUE.equals(invoiceRequired);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public UserRole getRole() {
        return role;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getBusinessUserId() {
        return businessUserId == null ? "U" + id : businessUserId;
    }

    public String getMemberLevel() {
        return memberLevel == null ? "normal" : memberLevel;
    }

    public String getRiskLevel() {
        return riskLevel == null ? "low" : riskLevel;
    }

    public String getPreferredCategories() {
        return preferredCategories == null ? "" : preferredCategories;
    }

    public String getPreferredDelivery() {
        return preferredDelivery == null ? "" : preferredDelivery;
    }

    public BigDecimal getBudgetMin() {
        return budgetMin;
    }

    public BigDecimal getBudgetMax() {
        return budgetMax;
    }

    public Boolean getInvoiceRequired() {
        return Boolean.TRUE.equals(invoiceRequired);
    }
}

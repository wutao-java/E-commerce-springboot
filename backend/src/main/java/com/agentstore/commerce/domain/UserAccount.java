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
    }

    public void updateProfile(String displayName, String phone, String address) {
        this.displayName = displayName;
        this.phone = phone;
        this.address = address;
    }

    public void changeBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
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
}

package com.agentstore.commerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentstore.commerce.dto.ApiModels.RegisterRequest;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
    }

    @Test
    void shouldRegisterWithEncryptedPassword() {
        var registered = authService.register(new RegisterRequest(
            "newbuyer", "buyer123", "新用户", "13800138000"));

        var account = userAccountRepository.findById(registered.id()).orElseThrow();
        assertThat(account.getPasswordHash()).isNotEqualTo("buyer123");
        assertThat(passwordEncoder.matches("buyer123", account.getPasswordHash())).isTrue();
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest("newbuyer", "buyer123", "新用户", "13800138000");
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户名");
    }
}

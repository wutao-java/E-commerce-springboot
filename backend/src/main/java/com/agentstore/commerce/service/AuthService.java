package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.dto.ApiModels.RegisterRequest;
import com.agentstore.commerce.dto.ApiModels.UserResponse;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount account = new UserAccount(username, passwordEncoder.encode(request.password()),
            request.displayName().trim(), request.phone().trim(), "", UserRole.CUSTOMER,
            BigDecimal.ZERO.setScale(2), LocalDateTime.now());
        return toUserResponse(userAccountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public UserAccount requireCurrentAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return userAccountRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效"));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Authentication authentication) {
        return toUserResponse(requireCurrentAccount(authentication));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        return new User(account.getUsername(), account.getPasswordHash(),
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
    }

    public UserResponse toUserResponse(UserAccount account) {
        return new UserResponse(account.getId(), account.getUsername(), account.getDisplayName(),
            account.getPhone(), account.getAddress(), account.getRole(), account.getBalance(), account.getCreatedAt(),
            account.getBusinessUserId(), account.getMemberLevel(), account.getRiskLevel(),
            account.getPreferredCategories(), account.getPreferredDelivery(), account.getBudgetMin(),
            account.getBudgetMax(), account.getInvoiceRequired());
    }
}

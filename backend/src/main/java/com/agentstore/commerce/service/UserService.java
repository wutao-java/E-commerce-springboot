package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.dto.ApiModels.BalanceRecordResponse;
import com.agentstore.commerce.dto.ApiModels.ProfileUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.UserResponse;
import com.agentstore.commerce.exception.BusinessException;
import com.agentstore.commerce.repository.BalanceRecordRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final BalanceRecordRepository balanceRecordRepository;
    private final AuthService authService;

    public UserService(UserAccountRepository userAccountRepository,
                       BalanceRecordRepository balanceRecordRepository, AuthService authService) {
        this.userAccountRepository = userAccountRepository;
        this.balanceRecordRepository = balanceRecordRepository;
        this.authService = authService;
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        UserAccount account = requireUser(userId);
        account.updateProfile(request.displayName().trim(), request.phone().trim(),
            request.address() == null ? "" : request.address().trim());
        return authService.toUserResponse(account);
    }

    @Transactional(readOnly = true)
    public List<BalanceRecordResponse> listBalanceRecords(Long userId) {
        return balanceRecordRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(record -> new BalanceRecordResponse(record.getId(), record.getType(), record.getAmount(),
                record.getBalanceAfter(), record.getDescription(), record.getCreatedAt()))
            .toList();
    }

    private UserAccount requireUser(Long userId) {
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
    }
}

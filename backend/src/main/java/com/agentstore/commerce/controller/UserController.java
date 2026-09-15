package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.BalanceRecordResponse;
import com.agentstore.commerce.dto.ApiModels.ProfileUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.UserResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.AuthService;
import com.agentstore.commerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户")
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping
    public ApiResponse<UserResponse> updateProfile(Authentication authentication,
                                                    @Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = authService.requireCurrentAccount(authentication).getId();
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @Operation(summary = "查询当前用户余额明细")
    @GetMapping("/balance-records")
    public ApiResponse<List<BalanceRecordResponse>> listBalanceRecords(Authentication authentication) {
        Long userId = authService.requireCurrentAccount(authentication).getId();
        return ApiResponse.success(userService.listBalanceRecords(userId));
    }
}

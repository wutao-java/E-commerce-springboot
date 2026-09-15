package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.CustomerServiceRequest;
import com.agentstore.commerce.dto.ApiModels.CustomerServiceResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.AuthService;
import com.agentstore.commerce.service.CustomerServiceGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商城客服")
@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private final AuthService authService;
    private final CustomerServiceGateway customerServiceGateway;

    public CustomerServiceController(AuthService authService, CustomerServiceGateway customerServiceGateway) {
        this.authService = authService;
        this.customerServiceGateway = customerServiceGateway;
    }

    @Operation(summary = "携带当前用户和页面上下文咨询客服")
    @PostMapping("/chat")
    public ApiResponse<CustomerServiceResponse> chat(
        Authentication authentication, @Valid @RequestBody CustomerServiceRequest request) {
        return ApiResponse.success(customerServiceGateway.chat(
            authService.requireCurrentAccount(authentication), request));
    }
}

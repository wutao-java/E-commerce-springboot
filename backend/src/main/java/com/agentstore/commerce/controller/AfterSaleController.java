package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.AfterSaleResponse;
import com.agentstore.commerce.dto.ApiModels.CreateAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.ReturnShipmentRequest;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.AuthService;
import com.agentstore.commerce.service.CommerceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "售后")
@RestController
@RequestMapping("/api/after-sales")
public class AfterSaleController {

    private final CommerceService commerceService;
    private final AuthService authService;

    public AfterSaleController(CommerceService commerceService, AuthService authService) {
        this.commerceService = commerceService;
        this.authService = authService;
    }

    @Operation(summary = "查询用户售后申请")
    @GetMapping
    public ApiResponse<List<AfterSaleResponse>> list(Authentication authentication) {
        Long userId = authService.requireCurrentAccount(authentication).getId();
        return ApiResponse.success(commerceService.listAfterSales(userId));
    }

    @Operation(summary = "创建售后申请")
    @PostMapping
    public ApiResponse<AfterSaleResponse> create(Authentication authentication,
                                                  @Valid @RequestBody CreateAfterSaleRequest request) {
        Long userId = authService.requireCurrentAccount(authentication).getId();
        return ApiResponse.success(commerceService.createAfterSale(userId, request));
    }

    @Operation(summary = "提交退货物流信息")
    @PostMapping("/{afterSaleId}/return-shipment")
    public ApiResponse<AfterSaleResponse> submitReturnShipment(
        @PathVariable Long afterSaleId, Authentication authentication,
        @Valid @RequestBody ReturnShipmentRequest request) {
        Long userId = authService.requireCurrentAccount(authentication).getId();
        return ApiResponse.success(commerceService.submitReturnShipment(afterSaleId, userId, request));
    }
}

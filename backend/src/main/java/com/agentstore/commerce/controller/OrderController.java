package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.CreateOrderRequest;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.CommerceService;
import com.agentstore.commerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@Tag(name = "订单")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CommerceService commerceService;
    private final AuthService authService;

    public OrderController(CommerceService commerceService, AuthService authService) {
        this.commerceService = commerceService;
        this.authService = authService;
    }

    @Operation(summary = "从购物车创建订单")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(Authentication authentication,
                                                   @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(commerceService.createOrder(currentUserId(authentication), request));
    }

    @Operation(summary = "查询用户订单")
    @GetMapping
    public ApiResponse<List<OrderResponse>> listOrders(Authentication authentication) {
        return ApiResponse.success(commerceService.listOrders(currentUserId(authentication)));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{orderNo}")
    public ApiResponse<OrderResponse> getOrder(
        @PathVariable String orderNo,
        Authentication authentication) {
        return ApiResponse.success(commerceService.getOrder(orderNo, currentUserId(authentication)));
    }

    @Operation(summary = "取消待支付订单")
    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
        @PathVariable String orderNo,
        Authentication authentication) {
        return ApiResponse.success(commerceService.cancelOrder(orderNo, currentUserId(authentication)));
    }

    @Operation(summary = "使用账户余额支付订单")
    @PostMapping("/{orderNo}/pay")
    public ApiResponse<OrderResponse> payOrder(@PathVariable String orderNo, Authentication authentication) {
        return ApiResponse.success(commerceService.payOrder(orderNo, currentUserId(authentication)));
    }

    @Operation(summary = "确认收货")
    @PostMapping("/{orderNo}/confirm")
    public ApiResponse<OrderResponse> confirmOrder(@PathVariable String orderNo, Authentication authentication) {
        return ApiResponse.success(commerceService.confirmOrder(orderNo, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        return authService.requireCurrentAccount(authentication).getId();
    }
}

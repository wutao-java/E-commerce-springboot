package com.agentstore.commerce.controller;

import com.agentstore.commerce.domain.OrderStatus;
import com.agentstore.commerce.dto.ApiModels.AfterSaleResponse;
import com.agentstore.commerce.dto.ApiModels.BalanceAdjustRequest;
import com.agentstore.commerce.dto.ApiModels.ConfirmReceiptRequest;
import com.agentstore.commerce.dto.ApiModels.OrderResponse;
import com.agentstore.commerce.dto.ApiModels.ProductResponse;
import com.agentstore.commerce.dto.ApiModels.ProductSaveRequest;
import com.agentstore.commerce.dto.ApiModels.RegisterRequest;
import com.agentstore.commerce.dto.ApiModels.ReviewAfterSaleRequest;
import com.agentstore.commerce.dto.ApiModels.ShipOrderRequest;
import com.agentstore.commerce.dto.ApiModels.UserResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "后台管理")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "查询全部商品")
    @GetMapping("/products")
    public ApiResponse<List<ProductResponse>> listProducts() {
        return ApiResponse.success(adminService.listProducts());
    }

    @Operation(summary = "创建商品")
    @PostMapping("/products")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.success(adminService.createProduct(request));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/products/{productId}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long productId,
                                                       @Valid @RequestBody ProductSaveRequest request) {
        return ApiResponse.success(adminService.updateProduct(productId, request));
    }

    @Operation(summary = "查询全部订单")
    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> listOrders(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.success(adminService.listOrders(keyword, status));
    }

    @Operation(summary = "订单发货")
    @PostMapping("/orders/{orderNo}/ship")
    public ApiResponse<OrderResponse> shipOrder(@PathVariable String orderNo,
                                                 @Valid @RequestBody ShipOrderRequest request) {
        return ApiResponse.success(adminService.shipOrder(orderNo, request));
    }

    @Operation(summary = "查询全部售后申请")
    @GetMapping("/after-sales")
    public ApiResponse<List<AfterSaleResponse>> listAfterSales() {
        return ApiResponse.success(adminService.listAfterSales());
    }

    @Operation(summary = "审核售后申请")
    @PostMapping("/after-sales/{afterSaleId}/review")
    public ApiResponse<AfterSaleResponse> reviewAfterSale(
        @PathVariable Long afterSaleId, @Valid @RequestBody ReviewAfterSaleRequest request) {
        return ApiResponse.success(adminService.reviewAfterSale(afterSaleId, request));
    }

    @Operation(summary = "确认收到售后退货")
    @PostMapping("/after-sales/{afterSaleId}/confirm-receipt")
    public ApiResponse<AfterSaleResponse> confirmAfterSaleReceipt(
        @PathVariable Long afterSaleId, @Valid @RequestBody ConfirmReceiptRequest request) {
        return ApiResponse.success(adminService.confirmAfterSaleReceipt(afterSaleId, request));
    }

    @Operation(summary = "查询用户")
    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> listUsers(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listUsers(keyword));
    }

    @Operation(summary = "新增普通用户")
    @PostMapping("/users")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(adminService.createUser(request));
    }

    @Operation(summary = "调整用户余额")
    @PostMapping("/users/{userId}/balance")
    public ApiResponse<UserResponse> adjustBalance(@PathVariable Long userId,
                                                    @Valid @RequestBody BalanceAdjustRequest request) {
        return ApiResponse.success(adminService.adjustBalance(userId, request));
    }
}

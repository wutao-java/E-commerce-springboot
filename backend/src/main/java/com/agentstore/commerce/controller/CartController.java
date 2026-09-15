package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.CartItemCreateRequest;
import com.agentstore.commerce.dto.ApiModels.CartItemUpdateRequest;
import com.agentstore.commerce.dto.ApiModels.CartResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.CommerceService;
import com.agentstore.commerce.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@Tag(name = "购物车")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CommerceService commerceService;
    private final AuthService authService;

    public CartController(CommerceService commerceService, AuthService authService) {
        this.commerceService = commerceService;
        this.authService = authService;
    }

    @Operation(summary = "查询用户购物车")
    @GetMapping
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        return ApiResponse.success(commerceService.getCart(currentUserId(authentication)));
    }

    @Operation(summary = "添加购物车商品")
    @PostMapping("/items")
    public ApiResponse<CartResponse> addCartItem(Authentication authentication,
                                                 @Valid @RequestBody CartItemCreateRequest request) {
        return ApiResponse.success(commerceService.addCartItem(currentUserId(authentication), request));
    }

    @Operation(summary = "修改购物车商品数量")
    @PutMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateCartItem(
        @PathVariable Long itemId,
        Authentication authentication,
        @Valid @RequestBody CartItemUpdateRequest request) {
        return ApiResponse.success(commerceService.updateCartItem(currentUserId(authentication), itemId, request));
    }

    @Operation(summary = "删除购物车商品")
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> deleteCartItem(
        @PathVariable Long itemId,
        Authentication authentication) {
        return ApiResponse.success(commerceService.deleteCartItem(currentUserId(authentication), itemId));
    }

    private Long currentUserId(Authentication authentication) {
        return authService.requireCurrentAccount(authentication).getId();
    }
}

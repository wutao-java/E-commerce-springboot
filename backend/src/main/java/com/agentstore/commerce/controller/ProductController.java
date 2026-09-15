package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.ProductResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.CommerceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商品")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CommerceService commerceService;

    public ProductController(CommerceService commerceService) {
        this.commerceService = commerceService;
    }

    @Operation(summary = "查询在售商品")
    @GetMapping
    public ApiResponse<List<ProductResponse>> listProducts(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category) {
        return ApiResponse.success(commerceService.listProducts(keyword, category));
    }

    @Operation(summary = "查询商品详情")
    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(commerceService.getProduct(productId));
    }
}

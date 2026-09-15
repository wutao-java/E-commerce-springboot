package com.agentstore.commerce.controller;

import com.agentstore.commerce.dto.ApiModels.AfterSalePolicyResponse;
import com.agentstore.commerce.dto.ApiModels.FaqResponse;
import com.agentstore.commerce.dto.ApiResponse;
import com.agentstore.commerce.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "服务政策")
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @Operation(summary = "查询售后政策")
    @GetMapping("/policies")
    public ApiResponse<List<AfterSalePolicyResponse>> listPolicies() {
        return ApiResponse.success(contentService.listPolicies());
    }

    @Operation(summary = "查询常见问题")
    @GetMapping("/faqs")
    public ApiResponse<List<FaqResponse>> listFaqs() {
        return ApiResponse.success(contentService.listFaqs());
    }
}

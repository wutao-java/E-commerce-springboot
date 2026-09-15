package com.agentstore.commerce.service;

import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.dto.ApiModels.CustomerServiceRequest;
import com.agentstore.commerce.dto.ApiModels.CustomerServiceResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CustomerServiceGateway {

    private static final String FALLBACK = "客服服务暂时繁忙，您可以稍后再试，或联系人工客服继续处理。";

    private final RestClient restClient;

    public CustomerServiceGateway(RestClient.Builder builder,
                                  @Value("${app.agent.base-url:http://127.0.0.1:8000}") String baseUrl,
                                  @Value("${app.agent.connect-timeout-ms:2000}") int connectTimeoutMs,
                                  @Value("${app.agent.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public CustomerServiceResponse chat(UserAccount user, CustomerServiceRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
            ? UUID.randomUUID().toString() : request.sessionId();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("session_id", sessionId);
        body.put("runtime_user_id", user.getBusinessUserId());
        body.put("runtime_nickname", user.getDisplayName());
        body.put("runtime_member_level", user.getMemberLevel());
        body.put("runtime_risk_level", user.getRiskLevel());
        body.put("user_message", request.message().trim());
        body.put("runtime_context", request.pageContext() == null ? Map.of() : request.pageContext());
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post().uri("/chat").body(body).retrieve().body(Map.class);
            if (response == null || response.get("answer") == null) {
                return new CustomerServiceResponse(FALLBACK, sessionId, true, Map.of());
            }
            Object state = response.get("session_state");
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionState = state instanceof Map<?, ?> ? (Map<String, Object>) state : Map.of();
            return new CustomerServiceResponse(String.valueOf(response.get("answer")),
                String.valueOf(response.getOrDefault("session_id", sessionId)), false, sessionState);
        } catch (RestClientException exception) {
            return new CustomerServiceResponse(FALLBACK, sessionId, true, Map.of());
        }
    }
}

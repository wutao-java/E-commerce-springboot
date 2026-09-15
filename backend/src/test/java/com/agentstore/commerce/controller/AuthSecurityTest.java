package com.agentstore.commerce.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agentstore.commerce.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
    }

    @Test
    void shouldRejectAnonymousCartRequest() throws Exception {
        mockMvc.perform(get("/api/cart"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldCreateAuthenticatedSessionAfterRegistration() throws Exception {
        var result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"sessionbuyer","password":"buyer123","displayName":"会话用户","phone":"13800138000"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("sessionbuyer"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("sessionbuyer"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowAdminToCreateCustomer() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admincreated","password":"buyer123","displayName":"新增买家","phone":"13900139000"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("admincreated"))
            .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "CUSTOMER")
    void shouldRejectCustomerCreatingUser() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"forbiddenuser","password":"buyer123","displayName":"越权用户","phone":"13900139000"}
                    """))
            .andExpect(status().isForbidden());
    }
}

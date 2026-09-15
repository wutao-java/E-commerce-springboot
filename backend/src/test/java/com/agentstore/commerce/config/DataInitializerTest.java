package com.agentstore.commerce.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentstore.commerce.dto.ApiModels.CommerceProfileRequest;
import com.agentstore.commerce.repository.UserAccountRepository;
import com.agentstore.commerce.service.UserService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.seed-data=true")
class DataInitializerTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private UserService userService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void shouldPreserveSavedPreferencesWhenSeedDataRunsAgain() {
        var buyer = userAccountRepository.findByUsername("buyer").orElseThrow();
        userService.updateCommerceProfile(buyer.getId(), new CommerceProfileRequest(
            "运动装备", "京东物流", new BigDecimal("500.00"), new BigDecimal("2500.00"), false));

        dataInitializer.run(null);

        var reloaded = userAccountRepository.findByUsername("buyer").orElseThrow();
        assertThat(reloaded.getPreferredCategories()).isEqualTo("运动装备");
        assertThat(reloaded.getPreferredDelivery()).isEqualTo("京东物流");
        assertThat(reloaded.getBudgetMin()).isEqualByComparingTo("500.00");
        assertThat(reloaded.getBudgetMax()).isEqualByComparingTo("2500.00");
        assertThat(reloaded.getInvoiceRequired()).isFalse();
    }
}

package com.agentstore.commerce.config;

import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductRepository productRepository, UserAccountRepository userAccountRepository,
                           PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userAccountRepository.existsByUsername("admin")) {
            userAccountRepository.save(account("admin", "admin123", "商城管理员", "13800000000",
                UserRole.ADMIN, "0.00"));
        }
        if (!userAccountRepository.existsByUsername("buyer")) {
            userAccountRepository.save(account("buyer", "buyer123", "演示买家", "13800138000",
                UserRole.CUSTOMER, "10000.00"));
        }
        if (productRepository.count() == 0) {
            seedProducts();
        }
    }

    private void seedProducts() {
        productRepository.saveAll(List.of(
            product("DIGI-KEY-01", "Flow 机械键盘", "数码办公", "紧凑 84 键布局，热插拔轴体与三模连接。", "699.00", "599.00", 24,
                "/products/custom-mechanical-keyboard.jpg"),
            product("DIGI-AUD-02", "Orbit 降噪耳塞", "数码办公", "入耳式主动降噪与通透模式，适合通勤和专注工作。", "1299.00", null, 18,
                "/products/noise-cancelling-earbuds.jpg"),
            product("LIFE-COF-03", "Barista 自动咖啡机", "居家生活", "研磨、萃取与奶泡一体，快速完成日常咖啡。", "1899.00", "1699.00", 31,
                "/products/automatic-espresso-machine.jpg"),
            product("TRIP-BAG-04", "Field 户外电源", "户外出行", "多接口便携储能电源，支持露营照明与数码设备供电。", "2999.00", null, 15,
                "/products/portable-power-station.jpg"),
            product("PHOTO-CAM-05", "Frame 运动相机套装", "影像设备", "轻量防抖相机与常用配件，适合旅行和户外记录。", "2599.00", "2299.00", 7,
                "/products/action-camera-bundle.jpg"),
            product("SPORT-RUN-06", "Trail 户外智能手表", "运动装备", "运动轨迹、心率与睡眠监测，适合日常训练。", "1299.00", null, 22,
                "/products/outdoor-smartwatch.jpg"),
            product("HOME-LAMP-07", "Breeze 智能空气净化器", "居家生活", "实时空气质量显示与自动净化，适合卧室和书房。", "1599.00", "1499.00", 19,
                "/products/smart-air-purifier.jpg"),
            product("LIFE-WAT-08", "Pocket 氮化镓快充", "数码办公", "小巧多口充电器，可为手机与轻薄电脑快速供电。", "239.00", null, 42,
                "/products/gan-fast-charger.jpg")
        ));
    }

    private Product product(String sku, String name, String category, String description,
                            String price, String promotionPrice, int stock, String imageUrl) {
        return new Product(sku, name, category, description, new BigDecimal(price),
            promotionPrice == null ? null : new BigDecimal(promotionPrice), stock, imageUrl, true);
    }

    private UserAccount account(String username, String password, String displayName, String phone,
                                UserRole role, String balance) {
        return new UserAccount(username, passwordEncoder.encode(password), displayName, phone,
            "上海市浦东新区 Agent 路 1 号", role, new BigDecimal(balance), LocalDateTime.now());
    }
}

package com.agentstore.commerce.config;

import com.agentstore.commerce.domain.AfterSalePolicy;
import com.agentstore.commerce.domain.CustomerOrder;
import com.agentstore.commerce.domain.FaqEntry;
import com.agentstore.commerce.domain.LogisticsEvent;
import com.agentstore.commerce.domain.OrderItem;
import com.agentstore.commerce.domain.Product;
import com.agentstore.commerce.domain.ProductPromotion;
import com.agentstore.commerce.domain.UserAccount;
import com.agentstore.commerce.domain.UserRole;
import com.agentstore.commerce.repository.AfterSalePolicyRepository;
import com.agentstore.commerce.repository.FaqEntryRepository;
import com.agentstore.commerce.repository.LogisticsEventRepository;
import com.agentstore.commerce.repository.OrderRepository;
import com.agentstore.commerce.repository.ProductPromotionRepository;
import com.agentstore.commerce.repository.ProductRepository;
import com.agentstore.commerce.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductPromotionRepository promotionRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final LogisticsEventRepository logisticsEventRepository;
    private final AfterSalePolicyRepository policyRepository;
    private final FaqEntryRepository faqRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(ProductRepository productRepository, ProductPromotionRepository promotionRepository,
                           UserAccountRepository userAccountRepository, OrderRepository orderRepository,
                           LogisticsEventRepository logisticsEventRepository,
                           AfterSalePolicyRepository policyRepository, FaqEntryRepository faqRepository,
                           PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.promotionRepository = promotionRepository;
        this.userAccountRepository = userAccountRepository;
        this.orderRepository = orderRepository;
        this.logisticsEventRepository = logisticsEventRepository;
        this.policyRepository = policyRepository;
        this.faqRepository = faqRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureAccount("admin", "admin123", "商城管理员", "13800000000",
            UserRole.ADMIN, "0.00", "A1000", "normal", "low");
        ensureAccount("buyer", "buyer123", "演示买家", "13800138000",
            UserRole.CUSTOMER, "10000.00", "U1000", "normal", "low");
        UserAccount zhangsan = ensureAccount("zhangsan", "123456", "张三", "13800138001",
            UserRole.CUSTOMER, "100000.00", "U1001", "gold", "low");
        ensureAccount("lisi", "123456", "李四", "13800138002",
            UserRole.CUSTOMER, "100000.00", "U1002", "silver", "medium");
        ensureAccount("wangwu", "123456", "王五", "13800138003",
            UserRole.CUSTOMER, "100.00", "U1003", "normal", "high");

        if (productRepository.count() == 0) {
            seedProducts();
        }
        seedPromotions();
        seedPoliciesAndFaqs();
        seedCourseOrders(zhangsan);
    }

    private UserAccount ensureAccount(String username, String password, String displayName, String phone,
                                      UserRole role, String balance, String businessUserId,
                                      String memberLevel, String riskLevel) {
        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
        boolean created = account == null;
        if (created) {
            account = userAccountRepository.save(account(username, password, displayName, phone, role, balance));
        }
        if (account.getRole() == UserRole.CUSTOMER || role == UserRole.ADMIN) {
            account.updateCommerceProfile(businessUserId, memberLevel, riskLevel,
                created && role == UserRole.CUSTOMER ? "数码办公,居家生活" : account.getPreferredCategories(),
                created ? "顺丰速运" : account.getPreferredDelivery(),
                created ? new BigDecimal("100.00") : account.getBudgetMin(),
                created ? new BigDecimal("5000.00") : account.getBudgetMax(),
                created ? role == UserRole.CUSTOMER : account.getInvoiceRequired());
        }
        return account;
    }

    private void seedProducts() {
        productRepository.saveAll(List.of(
            product("DIGI-KEY-01", "Flow 机械键盘", "数码办公", "紧凑 84 键布局，热插拔轴体与三模连接。", "699.00", "599.00", 24,
                "/products/custom-mechanical-keyboard.jpg", "三模连接,热插拔,84 键", true),
            product("DIGI-AUD-02", "Orbit 降噪耳塞", "数码办公", "入耳式主动降噪与通透模式，适合通勤和专注工作。", "1299.00", null, 18,
                "/products/noise-cancelling-earbuds.jpg", "主动降噪,通透模式,长续航", true),
            product("LIFE-COF-03", "Barista 自动咖啡机", "居家生活", "研磨、萃取与奶泡一体，快速完成日常咖啡。", "1899.00", "1699.00", 31,
                "/products/automatic-espresso-machine.jpg", "研磨萃取一体,自动奶泡", true),
            product("TRIP-BAG-04", "Field 户外电源", "户外出行", "多接口便携储能电源，支持露营照明与数码设备供电。", "2999.00", null, 15,
                "/products/portable-power-station.jpg", "大容量,多接口,便携储能", true),
            product("PHOTO-CAM-05", "Frame 运动相机套装", "影像设备", "轻量防抖相机与常用配件，适合旅行和户外记录。", "2599.00", "2299.00", 7,
                "/products/action-camera-bundle.jpg", "防抖,防水,配件套装", true),
            product("SPORT-RUN-06", "Trail 户外智能手表", "运动装备", "运动轨迹、心率与睡眠监测，适合日常训练。", "1299.00", null, 22,
                "/products/outdoor-smartwatch.jpg", "运动轨迹,健康监测", true),
            product("HOME-LAMP-07", "Breeze 智能空气净化器", "居家生活", "实时空气质量显示与自动净化，适合卧室和书房。", "1599.00", "1499.00", 19,
                "/products/smart-air-purifier.jpg", "空气质量监测,自动净化", true),
            product("LIFE-WAT-08", "Pocket 氮化镓快充", "数码办公", "小巧多口充电器，可为手机与轻薄电脑快速供电。", "239.00", null, 42,
                "/products/gan-fast-charger.jpg", "65W 快充,多接口", true)
        ));
    }

    private void seedPromotions() {
        if (promotionRepository.count() > 0) {
            return;
        }
        productRepository.findAll().stream().limit(3).forEach(product -> promotionRepository.save(
            new ProductPromotion(product.getId(), "开学数码焕新", "member_discount",
                "金卡会员享课程专属价", product.getPrice().multiply(new BigDecimal("0.85")),
                "gold", "金卡会员自动生效", LocalDateTime.now().minusDays(7),
                LocalDateTime.now().plusDays(30), true)));
    }

    private void seedPoliciesAndFaqs() {
        if (policyRepository.count() == 0) {
            policyRepository.saveAll(List.of(
                new AfterSalePolicy("refund_before_shipping", "未发货退款",
                    "订单支付后、发货前可以申请退款。", "订单已支付且尚未发货", "已发货订单不适用",
                    "订单号和退款原因", true),
                new AfterSalePolicy("return_after_delivery", "签收后七天退货",
                    "签收后七天内，商品完好且不影响二次销售时支持退货。", "已签收且商品支持七天退货",
                    "定制商品或影响二次销售的商品", "商品照片和退货原因", true),
                new AfterSalePolicy("logistics_compensation", "物流异常补偿",
                    "物流长时间无更新或出现异常时可申请补偿。", "物流存在延迟或异常", "正常运输中",
                    "物流单号和异常说明", true)
            ));
        }
        if (faqRepository.count() == 0) {
            faqRepository.saveAll(List.of(
                new FaqEntry("payment", "支持哪些支付方式？", "当前演示商城支持账户余额模拟支付。"),
                new FaqEntry("invoice", "如何申请发票？", "可以在账户偏好中开启电子发票需求，并在下单备注中填写抬头。"),
                new FaqEntry("shipping", "一般多久发货？", "现货商品通常在 24 小时内发货，具体以物流轨迹为准。"),
                new FaqEntry("after_sale", "如何查询售后进度？", "可以在我的售后页面查看状态、处理意见和审批记录。"),
                new FaqEntry("promotion", "会员优惠怎么使用？", "满足会员等级和活动时间后，购物车会自动应用活动价。")
            ));
        }
    }

    private void seedCourseOrders(UserAccount user) {
        List<Product> products = productRepository.findAll();
        if (products.size() < 3) {
            return;
        }
        seedOrder("SO20260422081500002-a1000002", user, products.get(0), "PAID", null);
        seedOrder("SO20260420103000001-a1000001", user, products.get(1), "SHIPPED", "SFCOURSE1001");
        seedOrder("SO20260418092000003-a1000003", user, products.get(2), "COMPLETED", "SFCOURSE1002");
    }

    private void seedOrder(String orderNo, UserAccount user, Product product, String state, String trackingNo) {
        if (orderRepository.findByOrderNo(orderNo).isPresent()) {
            return;
        }
        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        CustomerOrder order = new CustomerOrder(orderNo, user.getId(), product.getSalePrice(), user.getDisplayName(),
            user.getPhone(), user.getAddress(), "课程演示订单", createdAt);
        order.addItem(new OrderItem(product.getId(), product.getSku(), product.getName(), product.getImageUrl(),
            product.getSalePrice(), 1, product.getSalePrice()));
        order.pay(createdAt.plusMinutes(5));
        if ("SHIPPED".equals(state) || "COMPLETED".equals(state)) {
            order.ship(trackingNo, createdAt.plusDays(1));
        }
        if ("COMPLETED".equals(state)) {
            order.complete(createdAt.plusDays(3));
        }
        order = orderRepository.save(order);
        if (trackingNo != null) {
            logisticsEventRepository.save(new LogisticsEvent(order.getId(), "顺丰速运", trackingNo,
                "SHIPPED", "课程演示订单已发货", createdAt.plusDays(1)));
            if ("COMPLETED".equals(state)) {
                logisticsEventRepository.save(new LogisticsEvent(order.getId(), "顺丰速运", trackingNo,
                    "DELIVERED", "课程演示订单已签收", createdAt.plusDays(3)));
            }
        }
    }

    private Product product(String sku, String name, String category, String description,
                            String price, String promotionPrice, int stock, String imageUrl,
                            String highlights, boolean returnable) {
        return new Product(sku, name, category, description, new BigDecimal(price),
            promotionPrice == null ? null : new BigDecimal(promotionPrice), stock, imageUrl, true,
            highlights, returnable, returnable ? "支持七天无理由退货" : "不支持七天无理由退货", "课程商城");
    }

    private UserAccount account(String username, String password, String displayName, String phone,
                                UserRole role, String balance) {
        return new UserAccount(username, passwordEncoder.encode(password), displayName, phone,
            "上海市浦东新区 Agent 路 1 号", role, new BigDecimal(balance), LocalDateTime.now());
    }
}
